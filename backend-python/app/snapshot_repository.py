import logging
import re
from datetime import datetime, timedelta
from typing import Optional

import pandas as pd
from pandas.api import types as pandas_types
from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine


logger = logging.getLogger(__name__)
_SAFE_TABLE = re.compile(r"^[A-Za-z0-9_]+$")


class SnapshotRepository:
    def __init__(self, analysis_engine: Engine):
        self.analysis_engine = analysis_engine

    def ensure_schema(self) -> None:
        statements = [
            """
            CREATE TABLE IF NOT EXISTS churn_inference_batch (
                batch_id VARCHAR(36) NOT NULL,
                status VARCHAR(20) NOT NULL,
                target_count BIGINT NOT NULL DEFAULT 0,
                success_count BIGINT NOT NULL DEFAULT 0,
                failure_count BIGINT NOT NULL DEFAULT 0,
                model_version VARCHAR(64) NULL,
                snapshot_version VARCHAR(64) NULL,
                data_as_of DATETIME(6) NULL,
                error_message TEXT NULL,
                started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                completed_at DATETIME(6) NULL,
                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                    ON UPDATE CURRENT_TIMESTAMP(6),
                PRIMARY KEY (batch_id)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS churn_prediction_staging (
                batch_id VARCHAR(36) NOT NULL,
                member_id BIGINT NOT NULL,
                churn_score DOUBLE NULL,
                risk_grade VARCHAR(20) NULL,
                model_version VARCHAR(64) NOT NULL,
                snapshot_version VARCHAR(64) NOT NULL,
                data_as_of DATETIME(6) NOT NULL,
                status VARCHAR(20) NOT NULL,
                error_message TEXT NULL,
                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                PRIMARY KEY (batch_id, member_id),
                KEY idx_churn_staging_status (batch_id, status, member_id)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS churn_prediction_current (
                member_id BIGINT NOT NULL,
                churn_score DOUBLE NOT NULL,
                risk_grade VARCHAR(20) NOT NULL,
                model_version VARCHAR(64) NOT NULL,
                snapshot_version VARCHAR(64) NOT NULL,
                data_as_of DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                    ON UPDATE CURRENT_TIMESTAMP(6),
                PRIMARY KEY (member_id),
                KEY idx_churn_current_risk (risk_grade, member_id)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS churn_prediction_history (
                snapshot_version VARCHAR(64) NOT NULL,
                member_id BIGINT NOT NULL,
                churn_score DOUBLE NOT NULL,
                risk_grade VARCHAR(20) NOT NULL,
                model_version VARCHAR(64) NOT NULL,
                data_as_of DATETIME(6) NOT NULL,
                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                PRIMARY KEY (snapshot_version, member_id),
                KEY idx_churn_history_created (created_at)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS churn_prediction_summary_current (
                risk_grade VARCHAR(20) NOT NULL,
                customer_count BIGINT NOT NULL,
                ratio DOUBLE NOT NULL,
                data_as_of DATETIME(6) NOT NULL,
                PRIMARY KEY (risk_grade)
            ) ENGINE=InnoDB
            """,
        ]
        with self.analysis_engine.begin() as connection:
            for statement in statements:
                connection.execute(text(statement))

    def begin_inference(
        self,
        batch_id: str,
        target_count: int,
        model_version: str,
        snapshot_version: str,
        data_as_of: datetime,
        retry_failures: bool = False,
    ) -> str:
        self.ensure_schema()
        with self.analysis_engine.begin() as connection:
            result = connection.execute(
                text(
                    """
                    INSERT IGNORE INTO churn_inference_batch (
                        batch_id, status, target_count, model_version,
                        snapshot_version, data_as_of
                    ) VALUES (:batch_id, 'BUILDING', :target_count, :model_version,
                              :snapshot_version, :data_as_of)
                    """
                ),
                {
                    "batch_id": batch_id,
                    "target_count": target_count,
                    "model_version": model_version,
                    "snapshot_version": snapshot_version,
                    "data_as_of": data_as_of,
                },
            )
            if result.rowcount == 1:
                return "owner"

            row = connection.execute(
                text("""
                    SELECT status, failure_count, updated_at
                    FROM churn_inference_batch WHERE batch_id = :batch_id
                """),
                {"batch_id": batch_id},
            ).mappings().one()
            if row["status"] == "COMPLETED":
                if not retry_failures or int(row["failure_count"] or 0) == 0:
                    return "completed"
                retry = connection.execute(
                    text(
                        """
                        UPDATE churn_inference_batch
                        SET status = 'BUILDING', success_count = 0, failure_count = 0,
                            error_message = NULL, completed_at = NULL
                        WHERE batch_id = :batch_id AND status = 'COMPLETED'
                          AND failure_count > 0
                        """
                    ),
                    {"batch_id": batch_id},
                )
                return "owner" if retry.rowcount == 1 else "in_progress"
            if row["status"] == "FAILED":
                retry = connection.execute(
                    text(
                        """
                        UPDATE churn_inference_batch
                        SET status = 'BUILDING', target_count = :target_count,
                            model_version = :model_version,
                            snapshot_version = :snapshot_version,
                            data_as_of = :data_as_of,
                            error_message = NULL, completed_at = NULL
                        WHERE batch_id = :batch_id AND status = 'FAILED'
                        """
                    ),
                    {
                        "batch_id": batch_id,
                        "target_count": target_count,
                        "model_version": model_version,
                        "snapshot_version": snapshot_version,
                        "data_as_of": data_as_of,
                    },
                )
                return "owner" if retry.rowcount == 1 else "in_progress"
            if (
                row["status"] == "BUILDING"
                and row["updated_at"]
                and row["updated_at"] < datetime.utcnow() - timedelta(minutes=35)
            ):
                recovered = connection.execute(
                    text(
                        """
                        UPDATE churn_inference_batch
                        SET target_count = :target_count,
                            model_version = :model_version,
                            snapshot_version = :snapshot_version,
                            data_as_of = :data_as_of,
                            error_message = 'Recovered stale BUILDING inference'
                        WHERE batch_id = :batch_id AND status = 'BUILDING'
                          AND updated_at = :previous_updated_at
                        """
                    ),
                    {
                        "batch_id": batch_id,
                        "target_count": target_count,
                        "model_version": model_version,
                        "snapshot_version": snapshot_version,
                        "data_as_of": data_as_of,
                        "previous_updated_at": row["updated_at"],
                    },
                )
                return "owner" if recovered.rowcount == 1 else "in_progress"
            return "in_progress"

    def write_churn_staging(
        self,
        batch_id: str,
        detail: pd.DataFrame,
        failures: pd.DataFrame,
        model_version: str,
        snapshot_version: str,
        data_as_of: datetime,
    ) -> None:
        successful = detail.copy()
        if not successful.empty:
            successful["batch_id"] = batch_id
            successful["model_version"] = model_version
            successful["snapshot_version"] = snapshot_version
            successful["data_as_of"] = data_as_of
            successful["status"] = "SUCCESS"
            successful["error_message"] = None

        failed = failures.copy()
        if not failed.empty:
            failed["batch_id"] = batch_id
            failed["churn_score"] = None
            failed["risk_grade"] = None
            failed["model_version"] = model_version
            failed["snapshot_version"] = snapshot_version
            failed["data_as_of"] = data_as_of
            failed["status"] = "FAILED"

        columns = [
            "batch_id",
            "member_id",
            "churn_score",
            "risk_grade",
            "model_version",
            "snapshot_version",
            "data_as_of",
            "status",
            "error_message",
        ]
        with self.analysis_engine.begin() as connection:
            connection.execute(
                text("DELETE FROM churn_prediction_staging WHERE batch_id = :batch_id"),
                {"batch_id": batch_id},
            )
            if not successful.empty:
                successful[columns].to_sql(
                    "churn_prediction_staging",
                    con=connection,
                    if_exists="append",
                    index=False,
                    chunksize=2000,
                    method="multi",
                )
            if not failed.empty:
                failed[columns].to_sql(
                    "churn_prediction_staging",
                    con=connection,
                    if_exists="append",
                    index=False,
                    chunksize=2000,
                    method="multi",
                )

    def complete_inference(self, batch_id: str, success_count: int, failure_count: int) -> None:
        with self.analysis_engine.begin() as connection:
            connection.execute(
                text(
                    """
                    UPDATE churn_inference_batch
                    SET status = 'COMPLETED', success_count = :success_count,
                        failure_count = :failure_count,
                        completed_at = CURRENT_TIMESTAMP(6)
                    WHERE batch_id = :batch_id
                    """
                ),
                {
                    "batch_id": batch_id,
                    "success_count": success_count,
                    "failure_count": failure_count,
                },
            )

    def fail_inference(self, batch_id: str, error: Exception) -> None:
        message = str(error)[:4000]
        with self.analysis_engine.begin() as connection:
            connection.execute(
                text(
                    """
                    UPDATE churn_inference_batch
                    SET status = 'FAILED', error_message = :error_message,
                        completed_at = CURRENT_TIMESTAMP(6)
                    WHERE batch_id = :batch_id
                    """
                ),
                {"batch_id": batch_id, "error_message": message},
            )

    def inference_result(self, batch_id: str) -> Optional[dict]:
        self.ensure_schema()
        with self.analysis_engine.connect() as connection:
            row = connection.execute(
                text(
                    """
                    SELECT batch_id, status, target_count, success_count, failure_count,
                           model_version, snapshot_version, data_as_of
                    FROM churn_inference_batch WHERE batch_id = :batch_id
                    """
                ),
                {"batch_id": batch_id},
            ).mappings().first()
        return dict(row) if row else None

    def write_versioned_frame(
        self,
        table_name: str,
        frame: pd.DataFrame,
        batch_id: str,
        snapshot_version: str,
        data_as_of: datetime,
    ) -> None:
        if frame.empty:
            return
        self._validate_table_name(table_name)
        versioned = frame.copy()
        versioned.columns = [str(column) for column in versioned.columns]
        versioned["batch_id"] = batch_id
        versioned["snapshot_version"] = snapshot_version
        versioned["data_as_of"] = data_as_of

        self._ensure_metadata_columns(table_name)
        self._ensure_frame_columns(table_name, versioned)
        with self.analysis_engine.begin() as connection:
            if inspect(connection).has_table(table_name):
                connection.execute(
                    text(f"DELETE FROM {table_name} WHERE batch_id = :batch_id"),
                    {"batch_id": batch_id},
                )
            versioned.to_sql(
                table_name,
                con=connection,
                if_exists="append",
                index=False,
                chunksize=2000,
                method="multi",
            )

    def clear_versioned_batch(self, table_names: list[str], batch_id: str) -> None:
        """Clear only an idempotent BUILDING batch; READY retention is owned by Spring publish."""
        with self.analysis_engine.begin() as connection:
            inspector = inspect(connection)
            for table_name in table_names:
                self._validate_table_name(table_name)
                if inspector.has_table(table_name):
                    connection.execute(
                        text(f"DELETE FROM {table_name} WHERE batch_id = :batch_id"),
                        {"batch_id": batch_id},
                    )

    def read_current_churn_detail(self) -> tuple[pd.DataFrame, dict]:
        self.ensure_schema()
        detail = pd.read_sql(
            """
            SELECT member_id, churn_score, risk_grade
            FROM churn_prediction_current
            ORDER BY churn_score DESC, member_id
            """,
            con=self.analysis_engine,
        )
        metadata = {"snapshot_version": None, "data_as_of": None, "model_version": None}
        if not detail.empty:
            with self.analysis_engine.connect() as connection:
                row = connection.execute(
                    text(
                        """
                        SELECT snapshot_version, data_as_of, model_version
                        FROM churn_prediction_current
                        ORDER BY data_as_of DESC LIMIT 1
                        """
                    )
                ).mappings().first()
                if row:
                    metadata = dict(row)
        return detail, metadata

    def read_current_churn_summary(self) -> pd.DataFrame:
        self.ensure_schema()
        return pd.read_sql(
            """
            SELECT risk_grade AS grade, customer_count AS count, ratio
            FROM churn_prediction_summary_current
            ORDER BY FIELD(risk_grade, 'DANGER', 'WARNING', 'SAFE')
            """,
            con=self.analysis_engine,
        )

    def read_current_churn(self) -> tuple[pd.DataFrame, pd.DataFrame, dict]:
        detail, metadata = self.read_current_churn_detail()
        return detail, self.read_current_churn_summary(), metadata

    def publish_legacy_churn(
        self,
        detail: pd.DataFrame,
        batch_id: str,
        model_version: str,
        snapshot_version: str,
        data_as_of: datetime,
    ) -> None:
        self.write_churn_staging(
            batch_id,
            detail,
            pd.DataFrame(columns=["member_id", "error_message"]),
            model_version,
            snapshot_version,
            data_as_of,
        )
        with self.analysis_engine.begin() as connection:
            connection.execute(text("DELETE FROM churn_prediction_current"))
            connection.execute(
                text(
                    """
                    INSERT INTO churn_prediction_current (
                        member_id, churn_score, risk_grade, model_version,
                        snapshot_version, data_as_of
                    )
                    SELECT member_id, churn_score, risk_grade, model_version,
                           snapshot_version, data_as_of
                    FROM churn_prediction_staging
                    WHERE batch_id = :batch_id AND status = 'SUCCESS'
                    """
                ),
                {"batch_id": batch_id},
            )
            connection.execute(text("DELETE FROM churn_prediction_summary_current"))
            connection.execute(
                text(
                    """
                    INSERT INTO churn_prediction_summary_current (
                        risk_grade, customer_count, ratio, data_as_of
                    )
                    SELECT risk_grade, COUNT(*),
                           ROUND(COUNT(*) * 100.0 / totals.total_count, 2),
                           MAX(data_as_of)
                    FROM churn_prediction_current
                    CROSS JOIN (
                        SELECT COUNT(*) AS total_count FROM churn_prediction_current
                    ) totals
                    GROUP BY risk_grade, totals.total_count
                    """
                )
            )

    def _ensure_metadata_columns(self, table_name: str) -> None:
        self._validate_table_name(table_name)
        inspector = inspect(self.analysis_engine)
        if not inspector.has_table(table_name):
            return
        existing = {column["name"] for column in inspector.get_columns(table_name)}
        alterations = {
            "batch_id": "VARCHAR(36) NULL",
            "snapshot_version": "VARCHAR(64) NULL",
            "data_as_of": "DATETIME(6) NULL",
        }
        with self.analysis_engine.begin() as connection:
            for column, definition in alterations.items():
                if column not in existing:
                    connection.execute(text(f"ALTER TABLE {table_name} ADD COLUMN {column} {definition}"))

    def _ensure_frame_columns(self, table_name: str, frame: pd.DataFrame) -> None:
        inspector = inspect(self.analysis_engine)
        if not inspector.has_table(table_name):
            return
        existing = {column["name"] for column in inspector.get_columns(table_name)}
        with self.analysis_engine.begin() as connection:
            for column in frame.columns:
                if column in existing:
                    continue
                if not _SAFE_TABLE.match(column):
                    raise ValueError(f"Unsafe snapshot column name: {column}")
                dtype = frame[column].dtype
                if pandas_types.is_integer_dtype(dtype):
                    sql_type = "BIGINT NULL"
                elif pandas_types.is_float_dtype(dtype):
                    sql_type = "DOUBLE NULL"
                elif pandas_types.is_bool_dtype(dtype):
                    sql_type = "BOOLEAN NULL"
                elif pandas_types.is_datetime64_any_dtype(dtype):
                    sql_type = "DATETIME(6) NULL"
                else:
                    sql_type = "TEXT NULL"
                connection.execute(
                    text(f"ALTER TABLE {table_name} ADD COLUMN `{column}` {sql_type}")
                )

    @staticmethod
    def _validate_table_name(table_name: str) -> None:
        if not _SAFE_TABLE.match(table_name):
            raise ValueError("Unsafe snapshot table name")
