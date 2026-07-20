import unittest

import pandas as pd

from app.churn.churn_preprocess import build_base_dataset


class ChurnPreprocessTest(unittest.TestCase):
    def test_features_join_by_member_and_same_base_date(self):
        dates = ["2026-07-16", "2026-07-17"]
        consultation = pd.DataFrame(
            {"member_id": [1, 1], "feature_base_date": dates, "consult": [1, 2]}
        )
        monetary = pd.DataFrame(
            {"member_id": [1, 1], "feature_base_date": dates, "monetary": [10, 20]}
        )
        lifecycle = pd.DataFrame(
            {"member_id": [1, 1], "feature_base_date": dates, "lifecycle": [100, 101]}
        )
        usage = pd.DataFrame(
            {"member_id": [1, 1], "feature_base_date": dates, "usage": [5, 6]}
        )
        member = pd.DataFrame({"member_id": [1], "status": ["ACTIVE"]})

        result = build_base_dataset(
            consultation, monetary, lifecycle, usage, member
        )

        self.assertEqual(2, len(result))
        self.assertEqual(["2026-07-16", "2026-07-17"], result["feature_base_date"].tolist())
        self.assertEqual([10, 20], result["monetary"].tolist())


if __name__ == "__main__":
    unittest.main()
