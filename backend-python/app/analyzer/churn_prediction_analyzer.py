import hashlib
import logging
from pathlib import Path
from typing import Optional

import joblib
import pandas as pd

from app.churn.churn_data_loader import (
    load_feature_consultation,
    load_feature_lifecycle,
    load_feature_monetary,
    load_feature_usage,
    load_member,
)
from app.churn.churn_preprocess import (
    add_derived_features,
    build_base_dataset,
    get_feature_columns,
)


logger = logging.getLogger(__name__)
BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = BASE_DIR / "churn" / "artifacts" / "best_model.pkl"


def _make_risk_grade(churn_score: float) -> str:
    if churn_score >= 0.7:
        return "DANGER"
    if churn_score >= 0.4:
        return "WARNING"
    return "SAFE"


def get_model_version() -> str:
    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"Trained churn model does not exist: {MODEL_PATH}")
    digest = hashlib.sha256()
    with MODEL_PATH.open("rb") as model_file:
        for block in iter(lambda: model_file.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _load_prediction_base_df(batch_id: Optional[str] = None) -> pd.DataFrame:
    consultation = load_feature_consultation(batch_id)
    monetary = load_feature_monetary(batch_id)
    lifecycle = load_feature_lifecycle(batch_id)
    usage = load_feature_usage(batch_id)
    member = load_member()

    df = build_base_dataset(
        consultation=consultation,
        monetary=monetary,
        lifecycle=lifecycle,
        usage=usage,
        member=member,
    )
    if not batch_id and not df.empty:
        feature_dates = pd.to_datetime(df["feature_base_date"], errors="coerce")
        df = df.loc[feature_dates == feature_dates.max()].copy()
    return add_derived_features(df)


def calculate_churn_prediction(
    ojo_engine=None,
    batch_id: Optional[str] = None,
    chunk_size: int = 2000,
):
    if not MODEL_PATH.exists():
        raise FileNotFoundError(
            f"Trained model file does not exist: {MODEL_PATH}. "
            "Training remains a separate operation: python -m app.churn.churn_train"
        )

    model = joblib.load(MODEL_PATH)
    if not hasattr(model, "predict_proba"):
        raise ValueError("The deployed churn model does not support predict_proba")

    df = _load_prediction_base_df(batch_id)
    empty_detail = pd.DataFrame(columns=["member_id", "churn_score", "risk_grade"])
    empty_summary = pd.DataFrame(columns=["grade", "count", "ratio"])
    empty_failures = pd.DataFrame(columns=["member_id", "error_message"])
    if df.empty:
        return {
            "detail": empty_detail,
            "summary": empty_summary,
            "failures": empty_failures,
        }

    numeric_features, categorical_features, binary_features = get_feature_columns()
    feature_cols = numeric_features + categorical_features + binary_features
    missing_cols = [column for column in feature_cols if column not in df.columns]
    if missing_cols:
        raise ValueError(f"Missing inference columns: {missing_cols}")

    successes: list[pd.DataFrame] = []
    failures: list[dict] = []
    for offset in range(0, len(df), chunk_size):
        chunk = df.iloc[offset : offset + chunk_size]
        try:
            successes.append(_predict_chunk(model, chunk, feature_cols))
        except Exception as chunk_error:
            logger.exception(
                "Churn inference chunk failed; retrying members individually",
                extra={"batchId": batch_id, "offset": offset, "chunkSize": len(chunk)},
            )
            for _, row in chunk.iterrows():
                single = row.to_frame().T
                try:
                    successes.append(_predict_chunk(model, single, feature_cols))
                except Exception as member_error:
                    failures.append(
                        {
                            "member_id": int(row["member_id"]),
                            "error_message": str(member_error or chunk_error)[:4000],
                        }
                    )

    detail_df = pd.concat(successes, ignore_index=True) if successes else empty_detail
    failure_df = pd.DataFrame(failures, columns=["member_id", "error_message"])
    total = len(detail_df)
    summary_df = pd.DataFrame(
        [
            {
                "grade": grade,
                "count": int((detail_df["risk_grade"] == grade).sum()),
                "ratio": round(
                    (int((detail_df["risk_grade"] == grade).sum()) / total) * 100,
                    2,
                )
                if total
                else 0.0,
            }
            for grade in ["DANGER", "WARNING", "SAFE"]
        ]
    )
    return {"detail": detail_df, "summary": summary_df, "failures": failure_df}


def _predict_chunk(model, chunk: pd.DataFrame, feature_cols: list[str]) -> pd.DataFrame:
    churn_scores = model.predict_proba(chunk[feature_cols].copy())[:, 1]
    result = pd.DataFrame(
        {
            "member_id": chunk["member_id"].astype("int64").values,
            "churn_score": churn_scores,
        }
    )
    result["risk_grade"] = result["churn_score"].apply(_make_risk_grade)
    result["churn_score"] = result["churn_score"].round(4)
    return result[["member_id", "churn_score", "risk_grade"]]
