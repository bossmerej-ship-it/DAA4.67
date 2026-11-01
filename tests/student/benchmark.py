"""Compile separately, then measure only direct Java algorithm calls."""
import datetime
import hashlib
import json
import os
import pathlib
import platform
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from run import build


def capture_version(command):
    result = subprocess.run(command, check=True, capture_output=True, text=True, encoding="utf-8", timeout=10)
    return (result.stdout + result.stderr).strip()


def digest(path):
    # Git text checkout can change CRLF/LF; hash canonical UTF-8 text.
    return hashlib.sha256(path.read_text(encoding="utf-8").encode("utf-8")).hexdigest()


def main():
    build("Java")
    output = ROOT / "Java/build"
    subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", str(output),
                    "-d", str(output), str(ROOT / "tests/student/SearchBenchmark.java")], check=True, timeout=60)
    flags = ["-Xmx384m", "-Xbatch", "-XX:-TieredCompilation", "-XX:CompileThreshold=1000"]
    result = subprocess.run(["java", *flags, "-cp", str(output), "SearchBenchmark"],
                            check=True, capture_output=True, text=True, encoding="utf-8", timeout=60)
    destination = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "tests/student/benchmark.csv"
    destination.write_text(result.stdout, encoding="utf-8")
    metadata = {
        "recorded_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "platform": platform.platform(),
        "machine": platform.machine(),
        "processor": platform.processor(),
        "logical_processors": os.cpu_count(),
        "python": platform.python_version(),
        "java": capture_version(["java", "-version"]),
        "javac": capture_version(["javac", "-version"]),
        "java_flags": flags,
        "timing_scope": "Direct search call, preprocessing, result allocation and volatile result consumption",
        "excluded": "Compilation, JVM startup, warm-up, generation, validation, candidate counting and JSON I/O",
        "global_warmup_iterations": 10000,
        "calls_per_method_per_global_iteration": 3,
        "scenario_warmup_calls_per_method": 100,
        "measured_rounds": 3,
        "calls_per_sample": 50,
        "raw_sample_unit": "nanoseconds per 50 calls",
        "digest_normalization": "UTF-8 text with LF newlines",
        "source_sha256": {path: digest(ROOT / path) for path in (
            "Java/source/Algorithms.java", "tests/student/SearchBenchmark.java", "tests/student/benchmark.py")},
        "csv_sha256": digest(destination),
    }
    destination.with_suffix(".metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(result.stdout, end="")


if __name__ == "__main__":
    main()
