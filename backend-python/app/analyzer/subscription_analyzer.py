import pandas as pd
from sqlalchemy import text

def calculate_subscription(ojo_engine, data_as_of=None):
    query = """
    SELECT
        sp.member_id, 
        sp.product_id,
        p.product_name,
        sp.status,
        sp.started_at,
        sp.end_at,
        sp.reason_code
    FROM subscription_period sp
    JOIN product p ON sp.product_id = p.product_id
    WHERE (:data_as_of IS NULL OR sp.started_at < :data_as_of)
    ORDER BY sp.member_id, sp.started_at ASC
    """
    
    df = pd.read_sql(
        text(query), con=ojo_engine, params={"data_as_of": data_as_of}
    )
    
    if df.empty: return pd.DataFrame()
    
    df['started_at'] = pd.to_datetime(df['started_at'])
    df['start_month'] = df['started_at'].dt.strftime('%Y%m')
    
    # 요금제 전환율
    df['prev_product_id'] = df.groupby('member_id')['product_id'].shift(1)
    df['prev_product_name'] = df.groupby('member_id')['product_name'].shift(1)
    
    # 이전 제품이 있고, 현재 제품과 다르면 '전환'으로 간주
    df['is_conversion'] = (df['prev_product_id'].notnull()) & (df['prev_product_id'] != df['product_id'])

    conversion_stats = df[df['is_conversion']].groupby(['start_month', 'prev_product_name', 'product_name']).agg({
        'member_id': 'count'
    }).rename(columns={'member_id': 'conversion_count'}).reset_index()
    
    # 요금제 이탈률
    product_stats = df.groupby('product_name').agg(
        total_subs=('status', 'count'),
        churn_count=('status', lambda x: (x == 'CANCELED').sum())
    )

    product_stats['specific_churn_rate'] = (
        product_stats['churn_count'] / product_stats['total_subs']
    ).round(4)
    
    churn_result = product_stats.sort_values(by='specific_churn_rate', ascending=False).reset_index()
    top_reason = df[df['status'] == 'CANCLED'].groupby(['product_name', 'reason_code']).size().reset_index(name='count')
    
    return {
        "conversions": conversion_stats,
        "product_churn": churn_result,
        "top_reasons": top_reason
    }
