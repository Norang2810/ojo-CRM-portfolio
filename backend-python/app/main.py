from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse 
import pandas as pd
import numpy as np
import io
from urllib.parse import quote
from sqlalchemy import text

# 데이터베이스 및 분석 모듈
import time
import logging
import json
import uuid
from datetime import datetime, timezone
from pydantic import BaseModel

from .database import ojo_engine, analysis_engine
from .analyzer.ltv_analyzer import calculate_ltv
from .analyzer.cohort_analyzer import calculate_segmented_cohort
from .analyzer.advice_analyzer import get_member_advice_timeline
from .analyzer.subscription_analyzer import calculate_subscription
from .analyzer.regional_sales_analyzer import calculate_regional_sales
from .analyzer.churn_prediction_analyzer import calculate_churn_prediction, get_model_version
from .analyzer.rfm_analyzer import calculate_rfm_metrics
from .model.recommendation import get_recommendations, get_all_recommendations
from .snapshot_repository import SnapshotRepository
from .churn_summary_cache import ChurnSummaryCache

class JsonLogFormatter(logging.Formatter):
    def format(self, record):
        payload = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        for field in [
            "batchId", "targetCount", "successCount", "failureCount",
            "durationSeconds", "offset", "chunkSize", "segmentType"
        ]:
            if hasattr(record, field):
                payload[field] = getattr(record, field)
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


logging.basicConfig(level=logging.INFO)
for handler in logging.getLogger().handlers:
    handler.setFormatter(JsonLogFormatter())


app = FastAPI(title="High-5 Data Science Server")
logger = logging.getLogger(__name__)
snapshot_repository = SnapshotRepository(analysis_engine)
churn_summary_cache = ChurnSummaryCache()


class ChurnInferenceRequest(BaseModel):
    batchId: str
    featureBaseAt: datetime
    fullAnalysis: bool = False
    retryFailures: bool = False


def get_active_snapshot_version():
    try:
        with ojo_engine.connect() as connection:
            return connection.execute(
                text(
                    """
                    SELECT snapshot_version
                    FROM analytics_snapshot_manifest
                    WHERE active = TRUE AND status = 'READY'
                    ORDER BY activated_at DESC LIMIT 1
                    """
                )
            ).scalar()
    except Exception:
        return None


def stage_analysis_frame(
    frame: pd.DataFrame,
    batch_id: str,
    snapshot_version: str,
    data_as_of: datetime,
) -> None:
    columns = [
        'batch_id', 'member_id', 'snapshot_version', 'rfm_score', 'type',
        'ltv', 'lifecycle_stage', 'r_score', 'f_score', 'm_score', 'data_as_of'
    ]
    staged = frame.copy()
    staged['batch_id'] = batch_id
    staged['snapshot_version'] = snapshot_version
    staged['data_as_of'] = data_as_of
    with ojo_engine.begin() as connection:
        connection.execute(
            text("DELETE FROM analysis_staging WHERE batch_id = :batch_id"),
            {"batch_id": batch_id},
        )
        if not staged.empty:
            staged[columns].to_sql(
                'analysis_staging', con=connection, if_exists='append',
                index=False, chunksize=2000, method='multi'
            )
        if batch_id.startswith("legacy-"):
            connection.execute(text("DELETE FROM analysis_current"))
            connection.execute(
                text(
                    """
                    INSERT INTO analysis_current (
                        member_id, snapshot_version, rfm_score, type, ltv,
                        lifecycle_stage, r_score, f_score, m_score, data_as_of
                    )
                    SELECT member_id, snapshot_version, rfm_score, type, ltv,
                           lifecycle_stage, r_score, f_score, m_score, data_as_of
                    FROM analysis_staging WHERE batch_id = :batch_id
                    """
                ),
                {"batch_id": batch_id},
            )
            connection.execute(
                text(
                    """
                    INSERT INTO analysis (
                        member_id, rfm_score, type, ltv, lifecycle_stage,
                        created_at, r_score, f_score, m_score
                    )
                    SELECT member_id, rfm_score, type, ltv, lifecycle_stage,
                           data_as_of, r_score, f_score, m_score
                    FROM analysis_staging WHERE batch_id = :batch_id
                    """
                ),
                {"batch_id": batch_id},
            )


def run_versioned_full_analysis(
    batch_id: str,
    snapshot_version: str,
    data_as_of: datetime,
) -> None:
    started_at = time.perf_counter()
    logger.info("full_analysis_started", extra={"batchId": batch_id})

    snapshot_repository.clear_versioned_batch(
        [
            "ltv_snapshot",
            "cohort_snapshot",
            "conversion_snapshot",
            "churn_snapshot",
            "reason_snapshot",
            "region_snapshot",
        ],
        batch_id,
    )
    with ojo_engine.begin() as connection:
        connection.execute(
            text("DELETE FROM analysis_staging WHERE batch_id = :batch_id"),
            {"batch_id": batch_id},
        )

    ltv_df = calculate_ltv(ojo_engine, data_as_of=data_as_of)
    snapshot_repository.write_versioned_frame(
        'ltv_snapshot', ltv_df, batch_id, snapshot_version, data_as_of
    )

    cohorts = []
    for segment in ['all', 'high_consult', 'vip', 'big_spender']:
        cohort = calculate_segmented_cohort(
            ojo_engine, segment_type=segment, batch_id=batch_id
        )
        if not cohort.empty:
            cohorts.append(cohort)
    if cohorts:
        snapshot_repository.write_versioned_frame(
            'cohort_snapshot',
            pd.concat(cohorts, ignore_index=True),
            batch_id,
            snapshot_version,
            data_as_of,
        )

    subscription = calculate_subscription(ojo_engine, data_as_of=data_as_of)
    if isinstance(subscription, dict):
        for table_name, result_name in [
            ('conversion_snapshot', 'conversions'),
            ('churn_snapshot', 'product_churn'),
            ('reason_snapshot', 'top_reasons'),
        ]:
            frame = subscription[result_name]
            if not frame.empty:
                snapshot_repository.write_versioned_frame(
                    table_name, frame, batch_id, snapshot_version, data_as_of
                )

    rfm_metrics_df = calculate_rfm_metrics(
        ojo_engine, batch_id, data_as_of=data_as_of
    )
    if not ltv_df.empty and not rfm_metrics_df.empty:
        normalized_ltv = ltv_df.copy()
        normalized_ltv.columns = normalized_ltv.columns.str.lower()
        analysis_frame = pd.merge(
            normalized_ltv, rfm_metrics_df, on='member_id', how='left'
        )
        analysis_frame['rfm_score'] = analysis_frame['rfm_score'].fillna(0)
        analysis_frame['type'] = analysis_frame['type'].fillna('COMMON')
        analysis_frame['lifecycle_stage'] = analysis_frame['lifecycle_stage'].fillna('ACTIVE')
        analysis_frame['r_score'] = analysis_frame['r_score'].fillna(0)
        analysis_frame['f_score'] = analysis_frame['f_score'].fillna(0)
        analysis_frame['m_score'] = analysis_frame['m_score'].fillna(0)
        stage_analysis_frame(
            analysis_frame[[
                'member_id', 'ltv', 'rfm_score', 'type', 'lifecycle_stage',
                'r_score', 'f_score', 'm_score'
            ]],
            batch_id,
            snapshot_version,
            data_as_of,
        )

    region_stats = calculate_regional_sales(
        ojo_engine, analysis_engine, batch_id=batch_id
    )
    if region_stats:
        snapshot_repository.write_versioned_frame(
            'region_snapshot',
            pd.DataFrame(region_stats),
            batch_id,
            snapshot_version,
            data_as_of,
        )

    logger.info(
        "full_analysis_completed",
        extra={
            "batchId": batch_id,
            "durationSeconds": round(time.perf_counter() - started_at, 3),
        },
    )

origins = [
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "http://localhost:5173",
    "http://high5-ojo.s3-website.ap-northeast-2.amazonaws.com"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"], # GET, POST, OPTIONS 등 모든 메서드 허용
    allow_headers=["*"], # 모든 헤더 허용
)

# [분석 실행 로직] Spring이 호출함
def run_analysis_pipeline(
    batch_id: str,
    snapshot_version: str,
    data_as_of: datetime,
    include_churn: bool = False,
):
    start_time = time.time() 
    logger.info("legacy_analysis_started", extra={"batchId": batch_id})

    # 1. LTV 계산 및 저장
    logger.info("legacy_ltv_started", extra={"batchId": batch_id})
    ltv_df = calculate_ltv(ojo_engine)
    if not ltv_df.empty:
        snapshot_repository.write_versioned_frame(
            'ltv_snapshot', ltv_df, batch_id, snapshot_version, data_as_of
        )

    # 2. 코호트 세그먼트 리스트 정의 및 계산
    segments = ['all', 'high_consult', 'vip', 'big_spender']
    all_cohort_results = []

    for seg in segments:
        try:
            logger.info(
                "legacy_cohort_started",
                extra={"batchId": batch_id, "segmentType": seg},
            )
            df = calculate_segmented_cohort(ojo_engine, segment_type=seg)
            if not df.empty:
                all_cohort_results.append(df)
            else:
                logger.info(
                    "legacy_cohort_empty",
                    extra={"batchId": batch_id, "segmentType": seg},
                )
        except Exception:
            logger.exception(
                "legacy_cohort_failed",
                extra={"batchId": batch_id, "segmentType": seg},
            )

    if all_cohort_results:
        final_cohort_df = pd.concat(all_cohort_results, ignore_index=True)
        snapshot_repository.write_versioned_frame(
            'cohort_snapshot', final_cohort_df, batch_id, snapshot_version, data_as_of
        )
        logger.info(
            "legacy_cohort_completed",
            extra={"batchId": batch_id, "targetCount": len(final_cohort_df)},
        )

    # 3. 요금제별 이탈률 스냅샷 저장
    logger.info("legacy_subscription_started", extra={"batchId": batch_id})
    sub_result = calculate_subscription(ojo_engine)

    if isinstance(sub_result, dict):
        if not sub_result['conversions'].empty:
            snapshot_repository.write_versioned_frame(
                'conversion_snapshot', sub_result['conversions'],
                batch_id, snapshot_version, data_as_of
            )
        if not sub_result['product_churn'].empty:
            snapshot_repository.write_versioned_frame(
                'churn_snapshot', sub_result['product_churn'],
                batch_id, snapshot_version, data_as_of
            )
        if not sub_result['top_reasons'].empty:
            snapshot_repository.write_versioned_frame(
                'reason_snapshot', sub_result['top_reasons'],
                batch_id, snapshot_version, data_as_of
            )

    # 4. 이탈 예측 스냅샷 저장
    logger.info("legacy_churn_started", extra={"batchId": batch_id})
    try:
        churn_result = calculate_churn_prediction(ojo_engine) if include_churn else {
            "detail": pd.DataFrame(), "summary": pd.DataFrame(), "failures": pd.DataFrame()
        }

        logger.info(
            "legacy_churn_calculated",
            extra={
                "batchId": batch_id,
                "successCount": len(churn_result["detail"]),
                "failureCount": len(churn_result["failures"]),
            },
        )

        if not churn_result["detail"].empty:
            snapshot_repository.write_versioned_frame(
                'churn_prediction_snapshot', churn_result["detail"],
                batch_id, snapshot_version, data_as_of
            )

        if not churn_result["summary"].empty:
            snapshot_repository.write_versioned_frame(
                'churn_prediction_summary_snapshot', churn_result["summary"],
                batch_id, snapshot_version, data_as_of
            )
        logger.info("legacy_churn_completed", extra={"batchId": batch_id})

    except Exception:
        logger.exception("legacy_churn_failed", extra={"batchId": batch_id})
        raise

    if include_churn and batch_id.startswith("legacy-"):
        snapshot_repository.publish_legacy_churn(
            churn_result["detail"],
            batch_id,
            get_model_version(),
            snapshot_version,
            data_as_of,
        )

    # 6. 통합 analysis 테이블 생성 (RFM 기반 자동화)
    logger.info("legacy_analysis_merge_started", extra={"batchId": batch_id})
    try:
        rfm_metrics_df = calculate_rfm_metrics(
            ojo_engine, None if batch_id.startswith("legacy-") else batch_id
        )

        if not ltv_df.empty and not rfm_metrics_df.empty:
            ltv_df.columns = ltv_df.columns.str.lower()

            analysis_final_df = ltv_df.copy()
            analysis_final_df = pd.merge(
                analysis_final_df,
                rfm_metrics_df,
                on='member_id',
                how='left'
            )

            analysis_final_df['rfm_score'] = analysis_final_df['rfm_score'].fillna(0)
            analysis_final_df['type'] = analysis_final_df['type'].fillna('COMMON')
            analysis_final_df['lifecycle_stage'] = analysis_final_df['lifecycle_stage'].fillna('ACTIVE')
            analysis_final_df['created_at'] = datetime.now()
            analysis_final_df['r_score'] = analysis_final_df['r_score'].fillna(0)
            analysis_final_df['f_score'] = analysis_final_df['f_score'].fillna(0)
            analysis_final_df['m_score'] = analysis_final_df['m_score'].fillna(0) 

            stage_analysis_frame(
                analysis_final_df[[
                    'member_id', 'ltv', 'rfm_score', 'type', 'lifecycle_stage',
                    'r_score', 'f_score', 'm_score'
                ]],
                batch_id,
                snapshot_version,
                data_as_of,
            )

            logger.info("legacy_analysis_merge_completed", extra={"batchId": batch_id})
        else:
            logger.warning("legacy_analysis_merge_empty", extra={"batchId": batch_id})
    except Exception:
        logger.exception("legacy_analysis_merge_failed", extra={"batchId": batch_id})
        raise
        
    # 5. 지역별 분석 결과 스냅샷 저장
    logger.info("legacy_regional_started", extra={"batchId": batch_id})
    try:
        region_stats = calculate_regional_sales(ojo_engine, analysis_engine)
        if region_stats:
            region_df = pd.DataFrame(region_stats)
            snapshot_repository.write_versioned_frame(
                'region_snapshot', region_df, batch_id, snapshot_version, data_as_of
            )
            logger.info("legacy_regional_completed", extra={"batchId": batch_id})
    except Exception:
        logger.exception("legacy_regional_failed", extra={"batchId": batch_id})
        raise

    # 7. 맞춤 추천
    # try:
    #     print("[AI 추천] 고객별 맞춤 상품 추천 계산 시작...")
    #     get_all_recommendations(ojo_engine, analysis_engine) 
    #     print("[AI 추천] 추천 결과 스냅샷(recommend_snapshot) 적재 완료")
    # except Exception as e:
    #     print(f"[AI 추천] 추천 엔진 실행 중 에러 발생: {e}")

    logger.info(
        "legacy_analysis_completed",
        extra={
            "batchId": batch_id,
            "durationSeconds": round(time.time() - start_time, 3),
        },
    )

@app.post("/internal/v1/churn-inference")
def run_churn_inference(request: ChurnInferenceRequest):
    batch_id = request.batchId
    snapshot_version = batch_id
    data_as_of = request.featureBaseAt
    if data_as_of.tzinfo is not None:
        data_as_of = data_as_of.astimezone(timezone.utc).replace(tzinfo=None)
    model_version = get_model_version()

    with ojo_engine.connect() as connection:
        target_count = int(
            connection.execute(
                text(
                    """
                    SELECT COUNT(*) FROM analytics_batch_target
                    WHERE batch_id = :batch_id AND inference_required = TRUE
                    """
                ),
                {"batch_id": batch_id},
            ).scalar_one()
        )

    ownership = snapshot_repository.begin_inference(
        batch_id, target_count, model_version, snapshot_version, data_as_of,
        retry_failures=request.retryFailures,
    )
    if ownership == "completed":
        return _inference_response(
            snapshot_repository.inference_result(batch_id), already_completed=True
        )
    if ownership == "in_progress":
        raise HTTPException(status_code=409, detail="The same batchId is already being processed")

    started_at = time.perf_counter()
    try:
        result = calculate_churn_prediction(ojo_engine, batch_id=batch_id, chunk_size=2000)
        detail = result["detail"]
        failures = result["failures"]
        actual_count = len(detail) + len(failures)
        if actual_count != target_count:
            raise ValueError(
                f"Inference count mismatch: target={target_count}, actual={actual_count}"
            )

        snapshot_repository.write_churn_staging(
            batch_id, detail, failures, model_version, snapshot_version, data_as_of
        )
        if request.fullAnalysis:
            run_versioned_full_analysis(batch_id, snapshot_version, data_as_of)
        snapshot_repository.complete_inference(batch_id, len(detail), len(failures))
        stored = snapshot_repository.inference_result(batch_id)
        logger.info(
            "churn_inference_completed",
            extra={
                "batchId": batch_id,
                "targetCount": target_count,
                "successCount": len(detail),
                "failureCount": len(failures),
                "durationSeconds": round(time.perf_counter() - started_at, 3),
            },
        )
        return _inference_response(stored, already_completed=False)
    except Exception as error:
        snapshot_repository.fail_inference(batch_id, error)
        logger.exception("churn_inference_failed", extra={"batchId": batch_id})
        raise HTTPException(status_code=500, detail=str(error)) from error


def _inference_response(stored: dict, already_completed: bool) -> dict:
    data_as_of = stored.get("data_as_of")
    return {
        "status": "completed",
        "batchId": stored["batch_id"],
        "targetCount": int(stored["target_count"]),
        "successCount": int(stored["success_count"]),
        "failureCount": int(stored["failure_count"]),
        "modelVersion": stored.get("model_version"),
        "snapshotVersion": stored.get("snapshot_version"),
        "dataAsOf": _utc_iso(data_as_of),
        "alreadyCompleted": already_completed,
    }


def _utc_iso(value):
    if value is None:
        return None
    if isinstance(value, datetime):
        normalized = value if value.tzinfo else value.replace(tzinfo=timezone.utc)
        return normalized.astimezone(timezone.utc).isoformat()
    return value


@app.get("/api/analysis/make")
def make_analysis():
    batch_id = f"legacy-{uuid.uuid4().hex[:29]}"
    data_as_of = datetime.now(timezone.utc).replace(tzinfo=None)
    run_analysis_pipeline(batch_id, batch_id, data_as_of, include_churn=True)

    return {
        "status": "completed",
        "batchId": batch_id,
        "message": "다차원 분석 및 스냅샷 적재가 동기 방식으로 완료되었습니다."
    }

# 조회 API
@app.get("/api/analysis/ltv/{memberId}")
def get_member_ltv(memberId: str):
    snapshot_version = get_active_snapshot_version()
    query = text("""
        SELECT * FROM ltv_snapshot
        WHERE member_id = :memberId
          AND (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
        ORDER BY data_as_of DESC LIMIT 1
    """)
    df = pd.read_sql(
        query, 
        con=analysis_engine,
        params={"memberId": memberId, "snapshotVersion": snapshot_version}
    )
    if not df.empty:
        return {"status": "success", "data": df.to_dict(orient='records')[0]}
    else:
        return {"status": "error", "message": f"{memberId} ltv 조회 실패", "data": {}}


@app.get("/api/analysis/cohort")
def get_cohort(segment: str = 'all'):
    snapshot_version = get_active_snapshot_version()
    query = text("""
        SELECT * FROM cohort_snapshot
        WHERE segment_type = :segment
          AND (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
    """)
    df = pd.read_sql(
        query,
        con=analysis_engine,
        params={"segment": segment, "snapshotVersion": snapshot_version},
    )

    if df.empty:
        return {"status": "error", "message": f"No data found for segment: {segment}"}

    df = df.replace([np.inf, -np.inf], np.nan)
    result = df.to_dict(orient='records')
    clean_result = [{k: (None if pd.isna(v) else v) for k, v in record.items()} for record in result]

    return {"status": "success", "segment": segment, "data": clean_result}

# 요금제 통계
@app.get("/api/analysis/churn")
async def get_subscription():
    try:
        snapshot_version = get_active_snapshot_version()
        conversions = pd.read_sql(
            text("""
                SELECT * FROM conversion_snapshot
                WHERE (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
            """),
            con=analysis_engine,
            params={"snapshotVersion": snapshot_version},
        )
        churn = pd.read_sql(
            text("""
                SELECT * FROM churn_snapshot
                WHERE (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
            """),
            con=analysis_engine,
            params={"snapshotVersion": snapshot_version},
        )
        reasons = pd.read_sql(
            text("""
                SELECT * FROM reason_snapshot
                WHERE (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
            """),
            con=analysis_engine,
            params={"snapshotVersion": snapshot_version},
        )

        return {
            "status": "SUCCESS",
            "data": {
                "conversions": conversions.to_dict(orient='records'),
                "product_churn": churn.to_dict(orient='records'),
                "top_reasons": reasons.to_dict(orient='records')
            }
        }
    except Exception as e:
        return {"status": "ERROR", "message": f"데이터가 아직 준비되지 않았습니다: {str(e)}"}


# 지역 통계
@app.get("/api/analysis/region")
async def get_regional_sales():
    try:
        snapshot_version = get_active_snapshot_version()
        df = pd.read_sql(
            text("""
                SELECT * FROM region_snapshot
                WHERE (:snapshotVersion IS NULL OR snapshot_version = :snapshotVersion)
            """),
            con=analysis_engine,
            params={"snapshotVersion": snapshot_version},
        )
        return {"status": "SUCCESS", "data": df.to_dict(orient='records')}
    except Exception:
        return {"status": "ERROR", "message": "데이터를 불러올 수 없습니다."}

# 이탈률 예측
@app.get("/api/predictions/churn")
async def get_churn_prediction():
    try:
        detail_df, metadata = snapshot_repository.read_current_churn_detail()

        total_count = int(len(detail_df)) if not detail_df.empty else 0
        data_as_of = metadata.get("data_as_of")
        cached_summary = churn_summary_cache.get()
        if (
            cached_summary is None
            or cached_summary.get("snapshotVersion") != metadata.get("snapshot_version")
        ):
            summary_df = snapshot_repository.read_current_churn_summary()
            cached_summary = churn_summary_cache.put(
                summary_df.to_dict(orient="records") if not summary_df.empty else [],
                metadata,
                total_count,
            )

        return {
            "status": "success",
            "data": {
                "totalAnalyzed": total_count,
                "riskDistribution": cached_summary["riskDistribution"],
                "detail": detail_df.to_dict(orient="records") if not detail_df.empty else [],
                "snapshotVersion": cached_summary.get("snapshotVersion"),
                "dataAsOf": cached_summary.get("dataAsOf")
                    or (data_as_of.isoformat() if data_as_of else None),
                "modelVersion": cached_summary.get("modelVersion"),
                "stale": False
            },
            "message": None
        }

    except Exception as e:
        cached_summary = churn_summary_cache.get()
        if cached_summary is not None:
            return {
                "status": "success",
                "data": {
                    "totalAnalyzed": cached_summary.get("totalAnalyzed", 0),
                    "riskDistribution": cached_summary.get("riskDistribution", []),
                    "detail": [],
                    "snapshotVersion": cached_summary.get("snapshotVersion"),
                    "dataAsOf": cached_summary.get("dataAsOf"),
                    "modelVersion": cached_summary.get("modelVersion"),
                    "stale": True,
                },
                "message": "Current churn detail is unavailable; cached summary returned",
            }
        return {
            "status": "error",
            "data": None,
            "message": str(e)
        }


@app.post("/internal/v1/churn-cache/warm")
def warm_churn_summary_cache():
    detail, metadata = snapshot_repository.read_current_churn_detail()
    summary = snapshot_repository.read_current_churn_summary()
    payload = churn_summary_cache.put(
        summary.to_dict(orient="records") if not summary.empty else [],
        metadata,
        len(detail),
    )
    logger.info(
        "churn_summary_cache_warmed",
        extra={"targetCount": len(detail)},
    )
    return {"status": "completed", "snapshotVersion": payload.get("snapshotVersion")}

# 맞춤 상품 추천
@app.get("/api/analysis/recommend/{memberId}")
def get_member_recommendation(memberId: int):
    try:
        data = get_recommendations(memberId, ojo_engine)
        
        if not data:
            return {"status": "success", "data": [], "message": "추천 데이터가 없습니다."}

        return {
            "status": "success",
            "data": data
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

# 통합 고객 분석 데이터 조회 (LTV, RFM, 등급, 생애주기)
@app.get("/api/analysis/customer/{memberId}")
def get_customer_analysis(memberId: int):
    try:
        query = text("SELECT * FROM analysis_current WHERE member_id = :member_id")
        df = pd.read_sql(query, con=ojo_engine, params={"member_id": memberId})

        if df.empty:
            return {
                "status": "success",
                "data": {},
                "message": "해당 고객의 분석 데이터가 아직 없습니다."
            }

        result = df.to_dict(orient='records')[0]
        clean_result = {k: (None if pd.isna(v) else v) for k, v in result.items()}

        return {
            "status": "success",
            "data": clean_result,
            "message": "고객 통합 분석 데이터 조회 성공"
        }
    except Exception as e:
        return {
            "status": "error",
            "data": None,
            "message": f"분석 데이터 조회 중 오류 발생: {str(e)}"
        }
    
# 엑셀 보고서 다운로드 API
@app.get("/api/analysis/report/export")
def export_analysis_report():
    logger.info("integrated_report_started")
    try:
        an_df = pd.read_sql("SELECT * FROM analysis_current", con=ojo_engine)
        active_snapshot = get_active_snapshot_version()

        if an_df.empty:
            return {"status": "error", "message": "추출할 분석 데이터가 아직 없습니다. 파이프라인을 먼저 실행해주세요."}

        summary_data = {
            "총 고객 수": [len(an_df)],
            "VIP 고객 수": [len(an_df[an_df['type'] == 'VIP'])],
            "이탈 위험(RISK) 고객 수": [len(an_df[an_df['type'] == 'RISK'])],
            "평균 LTV (예측 수익)": [int(an_df['ltv'].mean()) if not an_df.empty else 0],
            "보고서 생성일시": [datetime.now().strftime("%Y-%m-%d %H:%M:%S")]
        }
        summary_df = pd.DataFrame(summary_data)

        output = io.BytesIO()
        with pd.ExcelWriter(output, engine='openpyxl') as writer:
            summary_df.to_excel(writer, sheet_name='핵심_요약', index=False)
            an_df.to_excel(writer, sheet_name='고객_분석_상세', index=False)

            # [코호트 분석]
            df_cohort = pd.read_sql(
                text("SELECT * FROM cohort_snapshot WHERE (:snapshot IS NULL OR snapshot_version = :snapshot)"),
                con=analysis_engine,
                params={"snapshot": active_snapshot},
            )
            if not df_cohort.empty:
                df_cohort.to_excel(writer, sheet_name="코호트_분석", index=False)
            else:
                pd.DataFrame({"안내": ["코호트 분석 데이터가 아직 수집되지 않았습니다."]}).to_excel(writer, sheet_name="코호트_분석", index=False)

            # [지역별 매출]
            df_region = pd.read_sql(
                text("SELECT * FROM region_snapshot WHERE (:snapshot IS NULL OR snapshot_version = :snapshot)"),
                con=analysis_engine,
                params={"snapshot": active_snapshot},
            )
            if not df_region.empty:
                df_region.to_excel(writer, sheet_name="지역별_매출", index=False)
            else:
                pd.DataFrame({"안내": ["지역별 매출 데이터가 아직 수집되지 않았습니다."]}).to_excel(writer, sheet_name="지역별_매출", index=False)

            # [LTV 분석]
            df_ltv = pd.read_sql(
                text("SELECT * FROM ltv_snapshot WHERE (:snapshot IS NULL OR snapshot_version = :snapshot)"),
                con=analysis_engine,
                params={"snapshot": active_snapshot},
            )
            if not df_ltv.empty:
                df_ltv.to_excel(writer, sheet_name="LTV_분석", index=False)
            else:
                pd.DataFrame({"안내": ["LTV 분석 데이터가 아직 수집되지 않았습니다."]}).to_excel(writer, sheet_name="LTV_분석", index=False)

            # [요금제 이탈률]
            df_churn = pd.read_sql(
                "SELECT * FROM churn_prediction_current",
                con=analysis_engine,
            )
            if not df_churn.empty:
                df_churn.to_excel(writer, sheet_name="요금제_이탈률", index=False)
            else:
                pd.DataFrame({"안내": ["요금제 이탈률 데이터가 아직 수집되지 않았습니다."]}).to_excel(writer, sheet_name="요금제_이탈률", index=False)

        output.seek(0)

        today_str = datetime.now().strftime("%Y%m%d")
        file_name = f"CRM_통합분석_보고서_{today_str}.xlsx"
        encoded_file_name = quote(file_name)

        headers = {
            'Content-Disposition': f'attachment; filename="{encoded_file_name}"'
        }

        return StreamingResponse(
            output, 
            headers=headers, 
            media_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        )

    except Exception as e:
        logger.exception("integrated_report_failed")
        return {"status": "error", "message": f"보고서 생성 중 오류가 발생했습니다: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
