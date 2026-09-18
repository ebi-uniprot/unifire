#!/usr/bin/env python3
"""Sanity checks for the bundled nextflow/versions.json.

The file drives runtime data-version resolution (see nextflow/versions.nf);
CI runs this to catch schema drift without a full pipeline run.

Usage:
    python3 nextflow/tests/test_versions.py
"""

import json
import unittest
from pathlib import Path

VERSIONS_JSON = Path(__file__).resolve().parents[1] / "versions.json"
REQUIRED_DATA_KEYS = {"uniprotRelease", "iprVersion", "iprscanVersion", "pirsrRelease"}


class VersionsJsonTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(VERSIONS_JSON.read_text())

    def test_parses_and_has_default(self):
        self.assertIsInstance(self.data.get("default"), str,
                              "'default' must be a version key string")

    def test_default_refs_existing_version(self):
        self.assertIn(self.data["default"], self.data["versions"],
                      "'default' must point to an existing version entry")

    def test_versions_non_empty(self):
        self.assertTrue(self.data.get("versions"),
                        "'versions' must contain at least one version")

    def test_every_version_has_required_keys(self):
        for key, cfg in self.data["versions"].items():
            missing = REQUIRED_DATA_KEYS - cfg.keys()
            self.assertEqual(set(), missing,
                             f"version '{key}' is missing {sorted(missing)}")

    def test_no_none_values(self):
        for key, cfg in self.data["versions"].items():
            empties = [k for k, v in cfg.items() if v is None or v == ""]
            self.assertEqual([], empties,
                             f"version '{key}' has empty values: {empties}")


if __name__ == "__main__":
    unittest.main(verbosity=2)
