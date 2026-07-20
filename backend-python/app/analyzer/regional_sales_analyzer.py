import pandas as pd
from sqlalchemy import text


def calculate_regional_sales(ojo_engine, analysis_engine, batch_id=None):
    if batch_id:
        region_query = text(
            """
            SELECT m.region, m.member_id,
                   COALESCE(f.total_revenue, 0) AS total_revenue,
                   COALESCE(f.monthly_revenue, 0) AS monthly_revenue,
                   a.type
            FROM member m
            JOIN feature_monetary_staging f
              ON m.member_id = f.member_id AND f.batch_id = :batch_id
            LEFT JOIN analysis_staging a
              ON m.member_id = a.member_id AND a.batch_id = :batch_id
            """
        )
        region_df = pd.read_sql(
            region_query, con=ojo_engine, params={"batch_id": batch_id}
        )
        churn_df = pd.read_sql(
            text(
                """
                SELECT member_id, risk_grade
                FROM churn_prediction_staging
                WHERE batch_id = :batch_id AND status = 'SUCCESS'
                """
            ),
            con=analysis_engine,
            params={"batch_id": batch_id},
        )
    else:
        with ojo_engine.connect() as connection:
            latest_date = connection.execute(
                text("SELECT MAX(feature_base_date) FROM feature_monetary")
            ).scalar()
        if not latest_date:
            return []

        region_df = pd.read_sql(
            text(
                """
                SELECT m.region, m.member_id,
                       COALESCE(f.total_revenue, 0) AS total_revenue,
                       COALESCE(f.monthly_revenue, 0) AS monthly_revenue,
                       a.type
                FROM member m
                JOIN feature_monetary f ON m.member_id = f.member_id
                LEFT JOIN analysis_current a ON m.member_id = a.member_id
                WHERE f.feature_base_date = :latest_date
                """
            ),
            con=ojo_engine,
            params={"latest_date": latest_date},
        )
        try:
            churn_df = pd.read_sql(
                "SELECT member_id, risk_grade FROM churn_prediction_current",
                con=analysis_engine,
            )
        except Exception:
            churn_df = pd.DataFrame(columns=["member_id", "risk_grade"])

    if region_df.empty:
        return []

    merged = region_df.merge(churn_df, on="member_id", how="left")
    merged["risk_grade"] = merged["risk_grade"].fillna("SAFE")
    stats = merged.groupby("region").agg(
        count=("member_id", "count"),
        totalRevenue=("total_revenue", "sum"),
        totalMonthlyRevenue=("monthly_revenue", "sum"),
        avgRevenue=("total_revenue", "mean"),
        avgMonthlyRevenue=("monthly_revenue", "mean"),
        vipCount=("type", lambda values: (values == "VIP").sum()),
        churnRiskCount=("risk_grade", lambda values: (values == "DANGER").sum()),
    ).reset_index()
    stats["ratio"] = (stats["count"] / len(merged) * 100).round(2)
    stats["churnRiskRatio"] = (
        stats["churnRiskCount"] / stats["count"].replace(0, 1) * 100
    ).round(2)
    return stats.to_dict(orient="records")
