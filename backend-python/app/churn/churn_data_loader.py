from typing import Optional

import pandas as pd
from sqlalchemy import text

from app.database import ojo_engine


def _load_feature(columns: str, table: str, batch_id: Optional[str]) -> pd.DataFrame:
    if batch_id:
        query = text(
            f"""
            SELECT {columns}
            FROM {table}_staging feature
            JOIN analytics_batch_target target
              ON target.batch_id = feature.batch_id
             AND target.member_id = feature.member_id
            WHERE feature.batch_id = :batch_id
              AND target.inference_required = TRUE
            """
        )
        return pd.read_sql(query, ojo_engine, params={"batch_id": batch_id})

    return pd.read_sql(f"SELECT {columns} FROM {table} feature", ojo_engine)


def load_feature_consultation(batch_id: Optional[str] = None) -> pd.DataFrame:
    return _load_feature(
        """
        feature.member_id,
        feature.feature_base_date,
        feature.total_consult_count,
        feature.last_7d_consult_count,
        feature.last_30d_consult_count,
        feature.avg_monthly_consult_count,
        feature.last_consult_date,
        feature.night_consult_count,
        feature.weekend_consult_count,
        feature.top_consult_category,
        feature.total_complaint_count,
        feature.last_consult_days_ago
        """,
        "feature_consultation",
        batch_id,
    )


def load_feature_monetary(batch_id: Optional[str] = None) -> pd.DataFrame:
    return _load_feature(
        """
        feature.member_id,
        feature.feature_base_date,
        feature.total_revenue,
        feature.last_payment_amount,
        feature.avg_monthly_bill,
        feature.last_payment_date,
        feature.payment_count_6m,
        feature.monthly_revenue,
        feature.payment_delay_count,
        feature.prev_monthly_revenue,
        feature.is_vip_prev_month,
        feature.avg_order_val,
        feature.purchase_cycle
        """,
        "feature_monetary",
        batch_id,
    )


def load_feature_lifecycle(batch_id: Optional[str] = None) -> pd.DataFrame:
    return _load_feature(
        """
        feature.member_id,
        feature.feature_base_date,
        feature.member_lifetime_days,
        feature.days_since_last_activity,
        feature.contract_end_days_left,
        feature.is_dormant_flag,
        feature.is_new_customer_flag,
        feature.is_terminated_flag,
        feature.signup_date
        """,
        "feature_lifecycle",
        batch_id,
    )


def load_feature_usage(batch_id: Optional[str] = None) -> pd.DataFrame:
    return _load_feature(
        """
        feature.member_id,
        feature.feature_base_date,
        feature.total_usage_amount,
        feature.avg_daily_usage,
        feature.max_usage_amount,
        feature.usage_peak_hour,
        feature.premium_service_count,
        feature.last_activity_date,
        feature.usage_active_days_30d
        """,
        "feature_usage",
        batch_id,
    )


def load_member() -> pd.DataFrame:
    return pd.read_sql(
        """
        SELECT member_id, gender, birth_date, region,
               household_type, status, created_at
        FROM member
        """,
        ojo_engine,
    )


def load_main_subscription_period() -> pd.DataFrame:
    return pd.read_sql(
        """
        SELECT sp.member_id, sp.started_at, sp.end_at,
               sp.status AS subscription_status,
               p.product_id, p.product_name, p.product_type, p.product_category
        FROM subscription_period sp
        JOIN product p ON sp.product_id = p.product_id
        WHERE p.product_category = 'BASE'
        """,
        ojo_engine,
    )
