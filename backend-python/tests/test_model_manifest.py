import json
import os
import unittest
from pathlib import Path


os.environ.setdefault("RDS_USERNAME", "test")
os.environ.setdefault("RDS_PASSWORD", "test")
os.environ.setdefault("RDS_HOST", "127.0.0.1")
os.environ.setdefault("RDS_PORT", "3306")
os.environ.setdefault("OJO_DATABASE", "ojo")
os.environ.setdefault("ANALYSIS_DATABASE", "ojo_analysis")

from app.analyzer.churn_prediction_analyzer import get_model_version


class ModelManifestTest(unittest.TestCase):
    def test_manifest_sha_matches_deployed_model(self):
        manifest_path = Path("app/churn/artifacts/model_manifest.json")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

        self.assertEqual(manifest["sha256"], get_model_version())
        self.assertEqual(
            "python -m app.churn.churn_train", manifest["trainingEntrypoint"]
        )


if __name__ == "__main__":
    unittest.main()
