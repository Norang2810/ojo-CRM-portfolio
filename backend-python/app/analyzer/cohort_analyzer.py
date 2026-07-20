import logging

import pandas as pd
from sqlalchemy import text


logger = logging.getLogger(__name__)


def calculate_segmented_cohort(ojo_engine, segment_type="all", batch_id=None):
    logger.info(
        "cohort_analysis_started",
        extra={"batchId": batch_id, "segmentType": segment_type},
    )

    if batch_id:
        query = text(
            """
            SELECT
                m.member_id,
                m.created_at AS join_date,
                i.created_at AS order_date,
                c.total_consult_count AS consult_count,
                mon.total_revenue AS total_amount,
                r.monetary AS rfm_monetary,
                r.frequency AS rfm_frequency
            FROM analytics_batch_target t
            JOIN analytics_batch_run b
              ON b.batch_id = t.batch_id
            JOIN member m
              ON m.member_id = t.member_id
            JOIN invoice i
              ON i.member_id = m.member_id
             AND i.created_at < b.feature_base_at
            LEFT JOIN feature_consultation_staging c
              ON c.member_id = m.member_id AND c.batch_id = t.batch_id
            LEFT JOIN feature_monetary_staging mon
              ON mon.member_id = m.member_id AND mon.batch_id = t.batch_id
            LEFT JOIN rfm_staging r
              ON r.member_id = m.member_id AND r.batch_id = t.batch_id
            WHERE t.batch_id = :batch_id
            """
        )
        frame = pd.read_sql(query, con=ojo_engine, params={"batch_id": batch_id})
    else:
        query = text(
            """
            SELECT
                m.member_id,
                m.created_at AS join_date,
                i.created_at AS order_date,
                c.total_consult_count AS consult_count,
                mon.total_revenue AS total_amount,
                r.monetary AS rfm_monetary,
                r.frequency AS rfm_frequency
            FROM member m
            JOIN invoice i ON i.member_id = m.member_id
            LEFT JOIN feature_consultation c
              ON c.member_id = m.member_id
             AND c.feature_base_date = (
                 SELECT MAX(c2.feature_base_date)
                 FROM feature_consultation c2
                 WHERE c2.member_id = m.member_id
             )
            LEFT JOIN feature_monetary mon
              ON mon.member_id = m.member_id
             AND mon.feature_base_date = (
                 SELECT MAX(mon2.feature_base_date)
                 FROM feature_monetary mon2
                 WHERE mon2.member_id = m.member_id
             )
            LEFT JOIN rfm r ON r.member_id = m.member_id
            """
        )
        frame = pd.read_sql(query, con=ojo_engine)

    if frame.empty:
        logger.info(
            "cohort_analysis_empty",
            extra={"batchId": batch_id, "segmentType": segment_type},
        )
        return pd.DataFrame()

    frame["join_date"] = pd.to_datetime(frame["join_date"])
    frame["order_date"] = pd.to_datetime(frame["order_date"])
    frame["consult_count"] = frame["consult_count"].fillna(0)
    frame["total_amount"] = frame["total_amount"].fillna(0)

    if segment_type == "high_consult":
        frame = frame[frame["consult_count"] >= 5]
    elif segment_type == "vip":
        frame = frame[frame["rfm_monetary"] >= 1_000_000]
    elif segment_type == "big_spender":
        frame = frame[frame["total_amount"] >= 1_000_000]

    if frame.empty:
        logger.info(
            "cohort_segment_empty",
            extra={"batchId": batch_id, "segmentType": segment_type},
        )
        return pd.DataFrame()

    frame["join_month"] = frame["join_date"].dt.to_period("M")
    frame["order_month"] = frame["order_date"].dt.to_period("M")
    frame["cohort_index"] = (
        frame["order_month"] - frame["join_month"]
    ).apply(lambda difference: difference.n)

    cohort_data = (
        frame.groupby(["join_month", "cohort_index"])["member_id"]
        .nunique()
        .reset_index()
    )
    cohort_pivot = cohort_data.pivot(
        index="join_month", columns="cohort_index", values="member_id"
    )
    if 0 not in cohort_pivot.columns:
        logger.warning(
            "cohort_initial_period_missing",
            extra={"batchId": batch_id, "segmentType": segment_type},
        )
        return pd.DataFrame()

    retention = cohort_pivot.divide(cohort_pivot[0], axis=0)
    retention.index = retention.index.astype(str)
    result = retention.reset_index()
    result["segment_type"] = segment_type

    logger.info(
        "cohort_analysis_completed",
        extra={
            "batchId": batch_id,
            "segmentType": segment_type,
            "targetCount": len(result),
        },
    )
    return result
