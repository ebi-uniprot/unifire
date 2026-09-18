#!/usr/bin/env python3
"""Unit tests for the UniFIRE Nextflow pipeline.

Runs the pipeline end-to-end with ``-stub-run`` and the ``test`` profile
(see ``nextflow/conf/profiles/test.config``): every process is replaced by
a lightweight stub, so no Docker images or remote data downloads are needed.
Invalid option combinations must fail fast with a clear error message.

Each test runs Nextflow in its own scratch directory, so runs never share
work directories or Nextflow state.

Usage: autodiscovered via ``python3 -m unittest discover -s nextflow/tests``
(or run directly: ``python3 nextflow/tests/test_smoke.py``).
"""

import os
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
MAIN_NF = REPO_ROOT / "main.nf"
PROFILE = "test"

NEXTFLOW_ENV = {
    **os.environ,
    "NXF_ANSI_LOG": "false",
    "NXF_DISABLE_CHECK_LATEST": "true",
}

# Input used by default in fail-fast tests (only the offending option is bad).
SAMPLE_FASTA = REPO_ROOT / "samples" / "proteins.fasta"

# Prediction files published by each inference system.
PREDICTIONS = {
    "unirule": "predictions_unirule.out",
    "arba": "predictions_arba.out",
    "pirsr": "predictions_unirule-pirsr.out",
}

# Invalid option combinations that must fail fast, without downloading any
# data. Each entry customizes the default command line built by
# SmokeTest._fail_fast:
#
#   invalid_options : offending command line options to append
#   input_file      : replaces the default sample FASTA as --input (optional)
#   expected_message: error fragment UNIFIRE must print and exit with
FAIL_FAST_CASES = {
    "bad-systems": dict(
        invalid_options=["--systems", "bogus"],
        expected_message="Invalid system(s)",
    ),
    "bad-output-format": dict(
        invalid_options=["--outputFormat", "CSV"],
        expected_message="Invalid output format",
    ),
    "bad-input-type": dict(
        invalid_options=["--inputType", "fasta2"],
        expected_message="Invalid input type",
    ),
    "bad-chunk-size": dict(
        invalid_options=["--chunkSize", "0"],
        expected_message="Invalid chunk size",
    ),
    "nonexistent-input": dict(
        input_file="nonexistent.fasta",
        expected_message="Input file does not exist",
    ),
    "bad-data-version": dict(
        invalid_options=["--version", "bogus"],
        expected_message="Version 'bogus' not found",
    ),
    "nonexistent-data-versions": dict(
        invalid_options=["--dataVersions", "nonexistent-versions.json"],
        expected_message="'--dataVersions' file does not exist",
    ),
}


class SmokeTest(unittest.TestCase):

    def setUp(self):
        # Isolated scratch dir per test: Nextflow cache/state of earlier runs
        # cannot interfere.
        tmp = tempfile.TemporaryDirectory(prefix="unifire-smoke-")
        self.addCleanup(tmp.cleanup)
        self.scratch = Path(tmp.name)
        self.run_dir = self.scratch / "run"
        self.run_dir.mkdir()

    # Invoking the pipeline ------------------------------------------------

    def run_pipeline(self, input_file=SAMPLE_FASTA, *, systems=None, input_type=None, iprscan_applications=None):
        """Run the pipeline (stubbed) and return the process.

        ``proc.output`` contains combined stdout+stderr. ``input_file`` is
        passed as ``--input`` when given (a falsy value omits ``--input`` so
        we can exercise the validation error). Output and dataPath dirs
        always point to per-test isolated paths.
        """
        cli_args = [
            "-work-dir", str(self.scratch / "work"),
            "-stub-run",
        ]
        if input_file:
            cli_args += ["--input", str(input_file)]
        cli_args += ["--output", str(self.scratch / "out"),
                     "--dataPath", str(self.scratch / "data")]
        if systems is not None:
            cli_args += ["--systems", systems]
        if input_type is not None:
            cli_args += ["--inputType", input_type]
        if iprscan_applications is not None:
            cli_args += ["--iprscanApplications", iprscan_applications]
        return self._nextflow(*cli_args)

    def _nextflow(self, *cli_args):
        cmd = ["nextflow", "run", str(MAIN_NF), "-profile", PROFILE, *cli_args]
        proc = subprocess.run(
            cmd,
            cwd=self.run_dir,
            env=NEXTFLOW_ENV,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=900,
        )
        proc.output = proc.stdout
        return proc

    # Assertions -----------------------------------------------------------

    def assert_outputs(self, *expected_names, out_dir=None):
        out_dir = Path(out_dir or self.scratch / "out")
        actual = sorted(p.name for p in out_dir.iterdir()) if out_dir.is_dir() else []
        self.assertEqual(
            actual, sorted(expected_names),
            f"wrong published files in {out_dir}",
        )

    def assert_run_succeeded(self, proc):
        self.assertEqual(
            proc.returncode, 0,
            f"expected zero exit, got {proc.returncode}\n"
            f"--- Nextflow output ---\n{proc.output}",
        )

    def assert_run_failed_with(self, proc, expected_message):
        self.assertNotEqual(
            proc.returncode, 0,
            f"expected non-zero exit, got 0\n"
            f"--- Nextflow output ---\n{proc.output}",
        )
        self.assertIn(expected_message, proc.output)

    # Static checks ---------------------------------------------------------

    def test_help_exits_zero_and_prints_usage(self):
        """--help prints the usage banner and exits successfully."""
        proc = self._nextflow("--help")
        self.assert_run_succeeded(proc)
        self.assertIn("UniProt Functional-Annotation", proc.output)

    # End-to-end stub runs ----------------------------------------------------

    def test_stub_fasta_all_systems(self):
        """FASTA input triggers InterProScan6 and publishes one file per system."""
        proc = self.run_pipeline(
            input_file=REPO_ROOT / "samples" / "proteins.fasta",
            systems="unirule,arba,pirsr",
        )
        self.assert_run_succeeded(proc)
        self.assert_outputs(PREDICTIONS["unirule"], PREDICTIONS["arba"],
                            PREDICTIONS["pirsr"])

    def test_stub_fasta_custom_iprscan_applications(self):
        """A custom --iprscanApplications value flows through the FASTA path."""
        proc = self.run_pipeline(
            input_file=REPO_ROOT / "samples" / "proteins.fasta",
            systems="unirule",
            iprscan_applications="HAMAP,Pfam",
        )
        self.assert_run_succeeded(proc)
        self.assert_outputs(PREDICTIONS["unirule"])

    def test_stub_interproscan_xml_unirule_arba(self):
        """InterProScan XML input with explicit input type publishes only unirule+arba."""
        proc = self.run_pipeline(
            input_file=REPO_ROOT / "test-data" / "proteins-ipr.xml",
            systems="unirule,arba",
            input_type="InterProScan",
        )
        self.assert_run_succeeded(proc)
        self.assert_outputs(PREDICTIONS["unirule"], PREDICTIONS["arba"])

    def test_stub_interproscan6_xml_infer_input_type(self):
        """Input type is inferred from the root element of an InterProScan6 XML."""
        proc = self.run_pipeline(
            input_file=REPO_ROOT / "test-data" / "proteins-ipr6.xml",
            systems="unirule",
        )
        self.assert_run_succeeded(proc)
        self.assert_outputs(PREDICTIONS["unirule"])

    def test_stub_single_system_isolation(self):
        """--systems unirule publishes only the unirule prediction file."""
        proc = self.run_pipeline(
            input_file=REPO_ROOT / "test-data" / "proteins-ipr.xml",
            systems="unirule",
        )
        self.assert_run_succeeded(proc)
        self.assert_outputs(PREDICTIONS["unirule"])

    # Fail-fast checks ----------------------------------------------------------

    def _assert_fail_fast(self, case_name):
        """Run with the options of a FAIL_FAST_CASES entry and expect its error."""
        case = FAIL_FAST_CASES[case_name]
        cli_args = [
            "-stub-run",
            "--input", case.get("input_file", SAMPLE_FASTA),
            "--output", str(self.scratch / "out"),
            "--dataPath", str(self.scratch / "data"),
        ]
        cli_args += case.get("invalid_options", [])
        cli_args = [str(a) for a in cli_args]
        proc = self._nextflow(*cli_args)
        self.assert_run_failed_with(proc, case["expected_message"])

    def test_bad_systems_fail_fast(self):
        """An unknown --systems value is rejected with the valid list."""
        self._assert_fail_fast("bad-systems")

    def test_bad_output_format_fail_fast(self):
        """An unknown --outputFormat value is rejected."""
        self._assert_fail_fast("bad-output-format")

    def test_bad_input_type_fail_fast(self):
        """An unknown --inputType value is rejected."""
        self._assert_fail_fast("bad-input-type")

    def test_bad_chunk_size_fail_fast(self):
        """A non-positive --chunkSize value is rejected."""
        self._assert_fail_fast("bad-chunk-size")

    def test_bad_data_version_fail_fast(self):
        """An unknown --version is rejected with the available version list."""
        self._assert_fail_fast("bad-data-version")

    def test_nonexistent_data_versions_fail_fast(self):
        """A missing --dataVersions file is rejected."""
        self._assert_fail_fast("nonexistent-data-versions")

    def test_nonexistent_input_fail_fast(self):
        """An --input path that does not exist is rejected."""
        self._assert_fail_fast("nonexistent-input")

    def test_missing_input_fail_fast(self):
        """Omitting --input entirely is rejected."""
        proc = self._nextflow(
            "-stub-run",
            "--output", str(self.scratch / "out"),
            "--dataPath", str(self.scratch / "data"),
        )
        self.assert_run_failed_with(proc, "is required")

    def test_uninferrable_input_type_fail_fast(self):
        """An input file that is neither FASTA nor recognizable XML is rejected."""
        uninferrable = self.scratch / "uninferrable.txt"
        uninferrable.touch()
        proc = self.run_pipeline(input_file=uninferrable)
        self.assert_run_failed_with(proc, "Could not infer input type")


if __name__ == "__main__":
    unittest.main(verbosity=2)
