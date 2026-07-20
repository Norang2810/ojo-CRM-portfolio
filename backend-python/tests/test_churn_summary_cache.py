import unittest
from datetime import datetime

from app.churn_summary_cache import ChurnSummaryCache


class FakeRedis:
    def __init__(self):
        self.values = {}
        self.ttls = {}

    def setex(self, key, ttl, value):
        self.values[key] = value
        self.ttls[key] = ttl

    def get(self, key):
        return self.values.get(key)


class ChurnSummaryCacheTest(unittest.TestCase):
    def test_round_trips_versioned_summary_with_ttl(self):
        cache = ChurnSummaryCache()
        fake_redis = FakeRedis()
        cache.client = fake_redis
        cache.ttl_seconds = 600

        cache.put(
            [{"grade": "DANGER", "count": 2, "ratio": 20.0}],
            {
                "snapshot_version": "snapshot-1",
                "data_as_of": datetime.fromisoformat("2026-07-18T12:00:00"),
                "model_version": "model-1",
            },
            10,
        )

        cached = cache.get()
        self.assertEqual("snapshot-1", cached["snapshotVersion"])
        self.assertEqual(10, cached["totalAnalyzed"])
        self.assertEqual(600, fake_redis.ttls[ChurnSummaryCache.KEY])


if __name__ == "__main__":
    unittest.main()
