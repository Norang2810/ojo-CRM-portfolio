import json
import logging
import os
from datetime import datetime, timezone
from typing import Optional

try:
    import redis
except ImportError:  # Local analysis-only environments can still fail open.
    redis = None


logger = logging.getLogger(__name__)


class ChurnSummaryCache:
    KEY = "churnSummaryCache::current"

    def __init__(self):
        self.ttl_seconds = int(os.getenv("CHURN_SUMMARY_CACHE_TTL_SECONDS", "600"))
        self.client = None if redis is None else redis.Redis(
            host=os.getenv("REDIS_HOST", "127.0.0.1"),
            port=int(os.getenv("REDIS_PORT", "6379")),
            decode_responses=True,
            socket_connect_timeout=1,
            socket_timeout=1,
        )

    def get(self) -> Optional[dict]:
        if self.client is None:
            return None
        try:
            value = self.client.get(self.KEY)
            return json.loads(value) if value else None
        except Exception:
            logger.warning("churn_summary_cache_read_failed", exc_info=True)
            return None

    def put(self, summary: list[dict], metadata: dict, total_count: int) -> dict:
        payload = {
            "totalAnalyzed": int(total_count),
            "riskDistribution": summary,
            "snapshotVersion": metadata.get("snapshot_version"),
            "dataAsOf": self._isoformat(metadata.get("data_as_of")),
            "modelVersion": metadata.get("model_version"),
            "cachedAt": datetime.now(timezone.utc).isoformat(),
        }
        if self.client is not None:
            try:
                self.client.setex(
                    self.KEY,
                    self.ttl_seconds,
                    json.dumps(payload, ensure_ascii=False),
                )
            except Exception:
                logger.warning("churn_summary_cache_write_failed", exc_info=True)
        return payload

    @staticmethod
    def _isoformat(value):
        if isinstance(value, datetime):
            normalized = value if value.tzinfo else value.replace(tzinfo=timezone.utc)
            return normalized.astimezone(timezone.utc).isoformat()
        return value.isoformat() if hasattr(value, "isoformat") else value
