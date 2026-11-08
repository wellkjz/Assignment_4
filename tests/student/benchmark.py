"""Reproduce the naive/KMP/RK comparison: python tests/student/benchmark.py"""
import json
import pathlib
import platform
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from run import build

build("Java")
output = ROOT / "Java" / "build"
subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", str(output),
                "-d", str(output), str(pathlib.Path(__file__).with_name("Benchmark.java"))],
               check=True, timeout=60)
result = subprocess.run(["java", "-Xmx384m", "-cp", str(output), "Benchmark"],
                        capture_output=True, text=True, encoding="utf-8", check=True, timeout=60)
report = json.loads(result.stdout)
report["os"] = platform.platform()
report["python"] = platform.python_version()
report["processor"] = platform.processor()
destination = pathlib.Path(__file__).with_name("benchmark_results.json")
destination.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print("case,n,m,algorithm,run1_us,run2_us,run3_us,median_us")
for row in report["rows"]:
    values = [str(row[key]) for key in ("case", "n", "m", "algorithm")]
    values += [f"{value / 100 / 1000:.3f}" for value in row["batch_ns"]]
    values.append(f"{row['median_ns_per_call'] / 1000:.3f}")
    print(",".join(values))
print(f"Saved {destination.name}")
