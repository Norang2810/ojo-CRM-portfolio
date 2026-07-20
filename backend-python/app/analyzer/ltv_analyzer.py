import logging

import pandas as pd
from sqlalchemy import text


logger = logging.getLogger(__name__)


def calculate_ltv(ojo_engine, data_as_of=None):
    logger.info("ltv_analysis_started")
    query = """
        SELECT m.member_id, i.billed_amount, i.created_at
        FROM member m
        JOIN invoice i ON m.member_id = i.member_id
    """
    params = None
    if data_as_of is not None:
        query += " WHERE i.created_at < :data_as_of"
        params = {"data_as_of": data_as_of}
    frame = pd.read_sql(text(query), con=ojo_engine, params=params)
    if frame.empty:
        return pd.DataFrame()

    ltv_base = frame.groupby("member_id").agg(
        avg_value=("billed_amount", "mean"),
        total_revenue=("billed_amount", "sum"),
        frequency=("billed_amount", "count"),
        first_order=("created_at", "min"),
        last_order=("created_at", "max"),
    )
    ltv_base["lifespan_days"] = (
        pd.to_datetime(ltv_base["last_order"])
        - pd.to_datetime(ltv_base["first_order"])
    ).dt.days
    ltv_base["LTV"] = ltv_base["avg_value"] * ltv_base["frequency"] * 1.2
    return ltv_base.drop(columns=["first_order", "last_order"]).reset_index()
