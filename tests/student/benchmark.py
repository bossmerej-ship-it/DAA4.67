"""Compile separately, then measure only direct Java algorithm calls."""
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from run import build


def main():
    build("Java")
    output = ROOT / "Java/build"
    subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", str(output),
                    "-d", str(output), str(ROOT / "tests/student/SearchBenchmark.java")], check=True, timeout=60)
    result = subprocess.run(["java", "-Xmx384m", "-Xbatch", "-XX:-TieredCompilation",
                             "-XX:CompileThreshold=1000", "-cp", str(output), "SearchBenchmark"],
                            check=True, capture_output=True, text=True, encoding="utf-8", timeout=60)
    destination = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "tests/student/benchmark.csv"
    destination.write_text(result.stdout, encoding="utf-8")
    print(result.stdout, end="")


if __name__ == "__main__":
    main()
