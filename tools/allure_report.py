"""Generate the Allure report from target/allure-results and open it in the browser.

Usage (from the project root, after "mvn test"):
    python tools/allure_report.py            # generate + serve on http://localhost:8080
    python tools/allure_report.py --port 9000
    python tools/allure_report.py --no-generate

The history of the previous report is carried over, so the Trend charts grow run after run.
The report is a single-page app that loads its data with fetch(), so it must be served
over HTTP; opening allure-report/index.html from disk shows an empty report.
"""

import argparse
import functools
import http.server
import shutil
import subprocess
import sys
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESULTS = ROOT / "target" / "allure-results"
REPORT = ROOT / "allure-report"


def generate() -> None:
    if not RESULTS.is_dir() or not any(RESULTS.iterdir()):
        sys.exit(f"No Allure results in {RESULTS} - run 'mvn test' first.")
    mvn = shutil.which("mvn") or shutil.which("mvn.cmd")
    if mvn is None:
        sys.exit("Maven ('mvn') is not on PATH.")
    previous_history = REPORT / "history"
    if previous_history.is_dir():
        # keeps the Trend / Retries / History tabs across runs
        shutil.copytree(previous_history, RESULTS / "history", dirs_exist_ok=True)
    subprocess.run([mvn, "-B", "-q", "allure:report"], cwd=ROOT, check=True)
    print(f"Report generated in {REPORT}")


def serve(port: int) -> None:
    if not (REPORT / "index.html").is_file():
        sys.exit(f"No report in {REPORT} - run without --no-generate.")
    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(REPORT))
    with http.server.ThreadingHTTPServer(("127.0.0.1", port), handler) as server:
        url = f"http://localhost:{port}/"
        print(f"Serving the Allure report at {url} (Ctrl+C to stop)")
        webbrowser.open(url)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--no-generate", action="store_true", help="serve the existing allure-report/ only")
    args = parser.parse_args()
    if not args.no_generate:
        generate()
    serve(args.port)


if __name__ == "__main__":
    main()
