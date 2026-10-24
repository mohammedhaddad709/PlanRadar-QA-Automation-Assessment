"""PlanRadar Form Builder - Test Dashboard (Python 3.10+, stdlib only).

    python runner/server.py                  ->  http://127.0.0.1:8090
    python runner/server.py --port 9000 --open
    python runner/server.py --stop           (stops the active dashboard and its test processes)

* Binds to 127.0.0.1 only. Compiles a code snapshot into runner/.build/<id> (classes, test classes, dependency
  classpath, AspectJ weaver) and launches TestNG directly from it, so source edits never disturb an active run.
* Every run owns runner/runs/<id>/ (run.json, console.log, suite.xml, output/, allure-report/). The run receives
  both -Doutput.dir and -Dallure.results.directory, so it never writes to target/ or the main allure-report/.
* One run at a time; a second request is rejected with HTTP 409.
* The payload is a whitelist (mode, browser, scope, headless override, demo options). Credentials are never
  accepted, shown, logged or stored: the Java side resolves them from -D / environment / the git-ignored local file.
* Every child process is started from an argument list (no shell), so -Dkey.with.dots=value is never re-parsed.
"""

import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import signal
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
import webbrowser
from datetime import datetime
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse
from xml.etree import ElementTree
from xml.sax.saxutils import quoteattr

RUNNER_DIR = Path(__file__).resolve().parent
ROOT = RUNNER_DIR.parent
sys.path.insert(0, str(ROOT / "tools"))

from gen_suites import plan  # noqa: E402
from systems import SYSTEMS  # noqa: E402
from testsource import discover  # noqa: E402

TITLE = "PlanRadar Form Builder \u2013 Test Dashboard"
HOST = "127.0.0.1"
RUNS_DIR = RUNNER_DIR / "runs"
BUILD_DIR = RUNNER_DIR / ".build"
STATIC_DIR = RUNNER_DIR / "static"
LOCK_FILE = RUNNER_DIR / ".runner.lock"
ENVIRONMENTS = json.loads((RUNNER_DIR / "environments.json").read_text(encoding="utf-8"))["systems"]
SYSTEM = next(iter(ENVIRONMENTS))
KEEP_SNAPSHOTS = 3
FINISHED = {"passed", "failed", "stopped", "error", "interrupted"}
ACTIVE = {"queued", "running", "stopping"}
BROWSERS = {"chrome", "edge", "firefox"}
RUN_MODES = {"demo", "normal", "fast"}
HEADLESS = {"auto", "true", "false"}
SCOPES = {"suite", "class", "method"}
PROGRESS = re.compile(r"\[PROGRESS\] (START|PASS|FAIL|SKIP) (\S+)#(\S+)")
OUTCOME = re.compile(r"\]\s+([A-Z][A-Z0-9]*-\d+) (PASSED|FAILED|BLOCKED|SKIPPED)\b")
TEST_ID = re.compile(r"\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)")
RUN_ID = re.compile(r"^\d{8}-\d{6}-[0-9a-f]{4}$")
# The Allure 2.x report template loads Google Analytics; per-run reports must work offline and stay local.
ANALYTICS = re.compile(r"<script async src=\"https://www\.googletagmanager\.com/[^<]*</script>\s*"
                       r"<script>\s*window\.dataLayer[\s\S]*?</script>\s*")

STATE_LOCK = threading.RLock()
RUNS = {}         # id -> run dict (persisted as run.json; never contains credentials)
PROCESSES = {}    # id -> subprocess.Popen
BUILD = {"status": "none", "snapshot": None, "fingerprint": None, "message": "", "started": None}
SNAPSHOTS = {}    # fingerprint -> snapshot id (ready)
CATALOG_CACHE = {"fingerprint": None, "value": None}


class BadRequest(Exception):
    status = HTTPStatus.BAD_REQUEST


class Conflict(BadRequest):
    status = HTTPStatus.CONFLICT


# ----------------------------------------------------------------------------- helpers

def now_iso():
    return datetime.now().isoformat(timespec="seconds")


def maven_command():
    names = ("mvn.cmd", "mvn") if os.name == "nt" else ("mvn",)
    executable = os.environ.get("MVN") or next((found for found in map(shutil.which, names) if found), None)
    if not executable:
        raise RuntimeError("Maven not found on PATH; set MVN to the mvn executable")
    return [executable]


def java_command():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")
        if candidate.is_file():
            return str(candidate)
    found = shutil.which("java")
    if not found:
        raise RuntimeError("java not found; install Java 21 or set JAVA_HOME")
    return found


def allure_command():
    """ALLURE_CLI, else the command line allure-maven downloads into .allure/, else 'allure' on PATH."""
    configured = os.environ.get("ALLURE_CLI")
    if configured and Path(configured).is_file():
        return [configured]
    script = "allure.bat" if os.name == "nt" else "allure"
    for bundled in sorted((ROOT / ".allure").glob("allure-2.*/bin/" + script), reverse=True):
        return [str(bundled)]
    found = shutil.which("allure")
    return [found] if found else None


def aspectj_version():
    match = re.search(r"<aspectj\.version>([^<]+)</aspectj\.version>", (ROOT / "pom.xml").read_text(encoding="utf-8"))
    return match.group(1).strip() if match else None


def source_fingerprint():
    digest = hashlib.sha1()
    files = [ROOT / "pom.xml"] + sorted(p for p in (ROOT / "src").rglob("*") if p.is_file())
    for path in files:
        stat = path.stat()
        digest.update(f"{path.relative_to(ROOT).as_posix()}|{stat.st_size}|{stat.st_mtime_ns}\n".encode())
    return digest.hexdigest()[:12]


def write_json(path, value):
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(value, indent=2, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, path)


def persist(run):
    run_dir = RUNS_DIR / run["id"]
    if run_dir.is_dir():
        write_json(run_dir / "run.json", run)


def process_alive(pid):
    if pid <= 0:
        return False
    if os.name == "nt":
        import ctypes
        handle = ctypes.windll.kernel32.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
        if not handle:
            return False
        code = ctypes.c_ulong()
        ctypes.windll.kernel32.GetExitCodeProcess(handle, ctypes.byref(code))
        ctypes.windll.kernel32.CloseHandle(handle)
        return code.value == 259  # STILL_ACTIVE
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def kill_tree(proc):
    """Java, the driver services and every browser they started."""
    if proc.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], capture_output=True)
    else:
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except OSError:
            proc.kill()


def duration_seconds(run):
    if not run.get("started"):
        return None
    end = datetime.fromisoformat(run["finished"]) if run.get("finished") else datetime.now()
    return max(0, int((end - datetime.fromisoformat(run["started"])).total_seconds()))


# ----------------------------------------------------------------------------- catalog

def credential_source():
    """Where the Java side will find the admin login. Only names are inspected; values are never read out."""
    names = [login["env"] for login in ENVIRONMENTS[SYSTEM]["logins"]]
    if all(os.environ.get(name) for name in names):
        return "environment variables"
    local = set()
    try:
        for line in (ROOT / "config" / "credentials.local.properties").read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#") and line.split("=", 1)[1].strip():
                local.add(line.split("=", 1)[0].strip())
    except OSError:
        pass
    if all(name in local for name in names):
        return "config/credentials.local.properties"
    return None


def catalog():
    fingerprint = source_fingerprint()
    if CATALOG_CACHE["fingerprint"] == fingerprint:
        return CATALOG_CACHE["value"]
    entries = discover()[SYSTEM]
    value = {
        "title": TITLE,
        "system": SYSTEM,
        "systemTitle": SYSTEMS[SYSTEM]["title"],
        "url": ENVIRONMENTS[SYSTEM]["envs"][next(iter(ENVIRONMENTS[SYSTEM]["envs"]))]["Web URL"],
        "classes": [{
            "class": e["class"], "simple": e["simple"], "feature": e["feature"],
            "methods": [{"name": m["name"], "id": m["id"], "title": m["description"]} for m in e["methods"] if m["enabled"]],
        } for e in entries],
        "suite": [c for _, classes in plan(SYSTEM, entries) for c in classes],
    }
    CATALOG_CACHE.update(fingerprint=fingerprint, value=value)
    return value


# ----------------------------------------------------------------------------- build snapshots

def load_snapshots():
    if not BUILD_DIR.is_dir():
        return
    ready = []
    for directory in BUILD_DIR.iterdir():
        marker = directory / "READY"
        if marker.is_file() and (directory / "FINGERPRINT").is_file():
            ready.append((marker.stat().st_mtime, (directory / "FINGERPRINT").read_text().strip(), directory.name))
    for _, fingerprint, snapshot in sorted(ready):
        SNAPSHOTS[fingerprint] = snapshot
        BUILD.update(status="ready", snapshot=snapshot, fingerprint=fingerprint)


def build_status():
    fingerprint = source_fingerprint()
    with STATE_LOCK:
        status = dict(BUILD)
        status["stale"] = BUILD["status"] != "building" and fingerprint not in SNAPSHOTS
        return status


def start_build(force=False):
    """Start an asynchronous snapshot build for the current sources. Returns the build status."""
    fingerprint = source_fingerprint()
    with STATE_LOCK:
        if BUILD["status"] == "building" or (fingerprint in SNAPSHOTS and not force):
            return build_status()
        snapshot = fingerprint + ("-" + datetime.now().strftime("%H%M%S") if force else "")
        BUILD.update(status="building", snapshot=snapshot, fingerprint=fingerprint, message="", started=now_iso())
    threading.Thread(target=_build, args=(fingerprint, snapshot), daemon=True).start()
    return build_status()


def _build(fingerprint, snapshot):
    directory = BUILD_DIR / snapshot
    if directory.exists():
        shutil.rmtree(directory, ignore_errors=True)
    directory.mkdir(parents=True)
    goals = ["test-compile", "dependency:build-classpath", "-Dmdep.outputFile=" + str(directory / "classpath.txt")]
    version = aspectj_version()
    if version:  # Allure @Step weaving; the weaver is a Surefire plugin dependency, not on the project classpath
        goals += ["dependency:copy", "-Dartifact=org.aspectj:aspectjweaver:" + version,
                  "-DoutputDirectory=" + str(directory / "agent")]
    command = maven_command() + ["-B", "-q"] + goals + ["-Dbuild.dir=" + str(directory / "target")]
    with open(directory / "build.log", "w", encoding="utf-8", errors="replace") as log:
        log.write("$ " + " ".join(command) + "\n")
        log.flush()
        code = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL).returncode
    ok = code == 0 and (directory / "classpath.txt").is_file() and (directory / "target" / "test-classes").is_dir()
    with STATE_LOCK:
        if ok:
            (directory / "FINGERPRINT").write_text(fingerprint)
            (directory / "READY").write_text(now_iso())
            SNAPSHOTS[fingerprint] = snapshot
            BUILD.update(status="ready", message="")
        else:
            tail = (directory / "build.log").read_text(encoding="utf-8", errors="replace")[-4000:]
            BUILD.update(status="failed", message=tail)
            for run in RUNS.values():
                if run["status"] == "queued":
                    finish(run, "error", "Build failed; see the build log")
    prune_snapshots()


def prune_snapshots():
    with STATE_LOCK:
        pinned = {r.get("snapshot") for r in RUNS.values() if r["status"] in ACTIVE}
        current = SNAPSHOTS.get(source_fingerprint())
        ready = sorted((p for p in BUILD_DIR.iterdir() if (p / "READY").is_file()),
                       key=lambda p: (p / "READY").stat().st_mtime, reverse=True) if BUILD_DIR.is_dir() else []
        for directory in ready[KEEP_SNAPSHOTS:]:
            if directory.name in pinned or directory.name == current:
                continue
            shutil.rmtree(directory, ignore_errors=True)
            for key, value in list(SNAPSHOTS.items()):
                if value == directory.name:
                    del SNAPSHOTS[key]


# ----------------------------------------------------------------------------- run creation

def suite_xml(name, blocks, method=None):
    lines = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">',
             '<suite name=%s verbose="1" parallel="false" configfailurepolicy="continue">' % quoteattr(name)]
    for block, classes in blocks:
        lines += ['    <test name=%s preserve-order="true">' % quoteattr(block), "        <classes>"]
        for class_name in classes:
            if method:
                lines += ["            <class name=%s>" % quoteattr(class_name),
                          "                <methods><include name=%s/></methods>" % quoteattr(method),
                          "            </class>"]
            else:
                lines.append("            <class name=%s/>" % quoteattr(class_name))
        lines += ["        </classes>", "    </test>"]
    return "\n".join(lines + ["</suite>"]) + "\n"


def choice(payload, key, allowed, default):
    value = str(payload.get(key, default)).strip().lower()
    if value not in allowed:
        raise BadRequest(f"'{key}' must be one of: {', '.join(sorted(allowed))}")
    return value


def create_run(payload):
    if not isinstance(payload, dict):
        raise BadRequest("Expected a JSON object")
    allowed = {"mode", "browser", "scope", "class", "method", "headless", "demoDelayMs", "demoHighlight"}
    unexpected = sorted(set(payload) - allowed)
    if unexpected:
        raise BadRequest(f"Unsupported field(s): {', '.join(unexpected)[:200]}. Credentials and custom properties "
                         "are not accepted here; the framework resolves credentials itself.")
    cat = catalog()
    classes = {c["class"]: c for c in cat["classes"]}
    mode = choice(payload, "mode", RUN_MODES, "normal")
    browser = choice(payload, "browser", BROWSERS, "chrome")
    headless = choice(payload, "headless", HEADLESS, "auto")
    scope = choice(payload, "scope", SCOPES, "suite")

    method = None
    if scope == "suite":
        blocks = [(name, list(group)) for name, group in plan(SYSTEM, discover()[SYSTEM])]
    else:
        class_name = payload.get("class")
        if class_name not in classes:
            raise BadRequest("Select a known test class")
        blocks = [(classes[class_name]["simple"], [class_name])]
        if scope == "method":
            method = payload.get("method")
            if method not in {m["name"] for m in classes[class_name]["methods"]}:
                raise BadRequest("Select a known test method of that class")
    blocks = [(name, group) for name, group in blocks if group]

    jvm = {"browser": browser, "run.mode": mode}
    for spec in ENVIRONMENTS[SYSTEM]["urls"]:
        for target in spec["targets"]:
            jvm[target] = cat["url"]
    if headless != "auto":
        jvm["headless"] = headless
    if mode == "demo":
        delay = payload.get("demoDelayMs", 800)
        if not isinstance(delay, int) or isinstance(delay, bool) or not 0 <= delay <= 5000:
            raise BadRequest("Demo step delay must be a whole number between 0 and 5000 ms")
        highlight = payload.get("demoHighlight", True)
        if not isinstance(highlight, bool):
            raise BadRequest("Demo highlight must be true or false")
        jvm["demo.step.delay.ms"] = str(delay)
        jvm["demo.highlight"] = str(highlight).lower()

    tests = []
    for _, group in blocks:
        for class_name in group:
            for m in classes[class_name]["methods"]:
                if method is None or m["name"] == method:
                    tests.append({"key": class_name + "#" + m["name"], "class": classes[class_name]["simple"],
                                  "method": m["name"], "id": m["id"], "title": m["title"], "status": "pending",
                                  "durationMs": None})
    if not tests:
        raise BadRequest("The selected scope contains no enabled tests")
    label = {"suite": "Full suite", "class": blocks[0][0], "method": f"{blocks[0][0]}#{method}"}[scope]

    with STATE_LOCK:
        active = next((r for r in RUNS.values() if r["status"] in ACTIVE), None)
        if active:
            raise Conflict(f"Run {active['id']} is still {active['status']}. Only one run at a time: "
                           "wait for it to finish or stop it.")
        run_id = datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + secrets.token_hex(2)
        run_dir = RUNS_DIR / run_id
        (run_dir / "output").mkdir(parents=True)
        (run_dir / "suite.xml").write_text(suite_xml(f"{cat['systemTitle']} - dashboard {run_id}", blocks, method),
                                           encoding="utf-8")
        run = {
            "id": run_id, "status": "queued", "message": "Queued", "created": now_iso(), "started": None,
            "finished": None, "mode": mode, "browser": browser, "headless": headless, "scope": scope,
            "scopeLabel": label, "demo": {"delayMs": int(jvm["demo.step.delay.ms"]), "highlight": jvm["demo.highlight"] == "true"}
            if mode == "demo" else None, "jvmProperties": jvm, "tests": tests,
            "progress": {"expected": len(tests), "done": 0, "passed": 0, "failed": 0, "blocked": 0, "skipped": 0,
                         "current": None},
            "results": None, "frameworkRunId": None, "snapshot": None, "exitCode": None, "allureReport": None,
            "preflight": [],
        }
        RUNS[run_id] = run
        persist(run)
    return run


# ----------------------------------------------------------------------------- scheduling and execution

def finish(run, status, message):
    run["status"] = status
    run["message"] = message
    run["finished"] = now_iso()
    run["durationSec"] = duration_seconds(run)
    persist(run)


def scheduler():
    while True:
        try:
            schedule_once()
        except Exception as error:  # keep the scheduler alive; surface the problem in the console
            print("scheduler error:", error, file=sys.stderr)
        time.sleep(1)


def schedule_once():
    with STATE_LOCK:
        candidate = next((r for r in RUNS.values() if r["status"] == "queued"), None)
        if candidate is None or any(r["status"] in ("running", "stopping") for r in RUNS.values()):
            return
        fingerprint = source_fingerprint()
        snapshot = SNAPSHOTS.get(fingerprint)
        if snapshot is None:
            if BUILD["status"] == "failed" and BUILD["fingerprint"] == fingerprint:
                finish(candidate, "error", "Build failed; fix the code or press Rebuild")
                return
            if candidate["message"] != "Compiling a code snapshot":
                candidate["message"] = "Compiling a code snapshot"
                persist(candidate)
            start_build()
            return
        candidate.update(status="running", snapshot=snapshot, started=now_iso(), message="Preflight checks")
        persist(candidate)
    threading.Thread(target=execute, args=(candidate,), daemon=True).start()


def preflight(url):
    """URL reachability only; authentication is exercised by every test (DEP-001)."""
    check = {"label": "Web URL", "url": url}
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "dashboard-preflight"}), timeout=20) as response:
            check.update(ok=response.status < 500, status=response.status)
    except urllib.error.HTTPError as error:
        check.update(ok=error.code < 500, status=error.code)
    except Exception as error:
        check.update(ok=False, status=None, detail=f"{type(error).__name__}: {error}")
    return check


def execute(run):
    run_dir = RUNS_DIR / run["id"]
    try:
        check = preflight(catalog()["url"])
        run["preflight"] = [check]
        if not check["ok"]:
            finish(run, "error", "Environment unreachable (preflight) - tests not started")
            return
        if run["status"] == "stopping":
            finish(run, "stopped", "Stopped by user before tests started")
            return
        snapshot_dir = BUILD_DIR / run["snapshot"]
        classpath_entries = (snapshot_dir / "classpath.txt").read_text(encoding="utf-8").strip().split(os.pathsep)
        weaver = next(iter(sorted((snapshot_dir / "agent").glob("aspectjweaver-*.jar"))), None)
        classpath = os.pathsep.join([str(snapshot_dir / "target" / "test-classes"),
                                     str(snapshot_dir / "target" / "classes")] + classpath_entries)
        output = run_dir / "output"
        properties = dict(run["jvmProperties"])
        properties.update({
            "active.system": ENVIRONMENTS[SYSTEM]["activeSystem"],
            "output.dir": str(output),
            "allure.results.directory": str(output / "allure-results"),
            "stdout.encoding": "UTF-8",
            "stderr.encoding": "UTF-8",
        })
        command = [java_command()]
        if weaver:
            command.append("-javaagent:" + str(weaver))
        command += ["-D" + key + "=" + value for key, value in properties.items()]
        command += ["-cp", classpath, "org.testng.TestNG", "-d", str(output / "testng"), str(run_dir / "suite.xml")]
        run["command"] = ["<snapshot " + run["snapshot"] + " classpath>" if part == classpath else part for part in command]
        run["message"] = "Running"
        persist(run)
        flags = {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP} if os.name == "nt" else {"start_new_session": True}
        with open(run_dir / "console.log", "w", encoding="utf-8", errors="replace", newline="") as console:
            proc = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    stdin=subprocess.DEVNULL, **flags)
            with STATE_LOCK:
                PROCESSES[run["id"]] = proc
                stop_requested = run["status"] == "stopping"
            if stop_requested:
                kill_tree(proc)
            for raw in proc.stdout:
                line = raw.decode("utf-8", errors="replace")
                console.write(line)
                console.flush()
                with STATE_LOCK:
                    track_progress(run, line)
            code = proc.wait()
        with STATE_LOCK:
            PROCESSES.pop(run["id"], None)
        run["exitCode"] = code
        run["results"] = parse_results(run_dir)
        run["frameworkRunId"] = framework_run_id(output)
        reconcile_tests(run)
        run["message"] = "Generating Allure report"
        persist(run)
        run["allureReport"] = generate_allure(run_dir)
        results = run["results"]
        if run["status"] == "stopping":
            finish(run, "stopped", "Stopped by user")
        elif results is None:
            finish(run, "error", f"TestNG produced no results (exit code {code}); see the log")
        elif results["failed"] or results["blocked"] or results["configFailures"]:
            parts = [f"{results[k]} {k}" for k in ("failed", "blocked") if results[k]]
            if results["configFailures"]:
                parts.append(f"{results['configFailures']} setup/teardown errors")
            finish(run, "failed", ", ".join(parts))
        elif results["skipped"] or not results["passed"]:
            finish(run, "failed", f"{results['passed']} passed, {results['skipped']} skipped")
        else:
            finish(run, "passed", f"All {results['passed']} tests passed")
    except Exception as error:
        finish(run, "error", f"Dashboard error: {type(error).__name__}: {error}")
    finally:
        prune_snapshots()


def track_progress(run, line):
    rows = {t["key"]: t for t in run["tests"]}
    outcome = OUTCOME.search(line)
    if outcome:
        test_id, status = outcome.groups()
        for row in run["tests"]:
            if row["id"] == test_id and row["status"] in ("pending", "running"):
                row["status"] = status.lower()
                break
        return
    match = PROGRESS.search(line)
    if not match:
        return
    status, class_name, method = match.groups()
    row = rows.get(class_name + "#" + method)
    progress = run["progress"]
    if status == "START":
        progress["current"] = class_name.rsplit(".", 1)[-1] + "#" + method
        if row:
            row["status"] = "running"
        return
    fallback = {"PASS": "passed", "FAIL": "failed", "SKIP": "skipped"}[status]
    if row and row["status"] in ("pending", "running"):
        row["status"] = fallback
    final = row["status"] if row else fallback
    progress["done"] += 1
    progress[final if final in ("passed", "failed", "blocked", "skipped") else fallback] += 1
    progress["current"] = None


def reconcile_tests(run):
    by_key = {f"{t['class']}#{t['method']}": t for t in (run.get("results") or {}).get("tests", [])}
    for row in run["tests"]:
        result = by_key.get(row["key"])
        if result:
            row["durationMs"] = result["durationMs"]
            row["status"] = result["status"]
            row["screenshots"] = result["screenshots"]
        elif row["status"] in ("pending", "running"):
            row["status"] = "not run"


def parse_results(run_dir):
    path = run_dir / "output" / "testng" / "testng-results.xml"
    if not path.is_file():
        return None
    root = ElementTree.parse(path).getroot()
    totals = {"passed": 0, "failed": 0, "blocked": 0, "skipped": 0}
    failures, tests = [], []
    screenshots = list_screenshots(run_dir)
    for cls in root.iter("class"):
        for method in cls.iter("test-method"):
            config = method.get("is-config") == "true"
            status = method.get("status")
            if config and status == "PASS":
                continue
            name = method.get("name")
            description = method.get("description") or ""
            exception = method.find("exception")
            message = (exception.findtext("message") or "").strip() if exception is not None else ""
            if not config:
                state = {"PASS": "passed", "FAIL": "failed"}.get(status) or ("blocked" if message.startswith("BLOCKED") else "skipped")
                totals[state] += 1
                tests.append({"class": cls.get("name"), "method": name, "status": state, "description": description,
                              "durationMs": int(method.get("duration-ms") or 0),
                              "screenshots": matching_screenshots(screenshots, name, description)})
            if status == "FAIL" or (status == "SKIP" and exception is not None):
                trace = (exception.findtext("full-stacktrace") or "") if exception is not None else ""
                failures.append({
                    "class": cls.get("name"), "method": name, "config": config, "status": status,
                    "description": description, "exception": exception.get("class") if exception is not None else "",
                    "message": message[:4000], "trace": "\n".join(trace.strip().splitlines()[:25]),
                })
    summary = run_dir / "output" / "execution-summary.md"
    return {**totals, "total": sum(totals.values()),
            "configFailures": sum(1 for f in failures if f["config"] and f["status"] == "FAIL"),
            "failures": failures, "tests": tests, "screenshots": screenshots,
            "summary": summary.relative_to(run_dir).as_posix() if summary.is_file() else None}


def matching_screenshots(screenshots, method, description):
    """Screenshots are named '<test id>_<status>_<time>.png'."""
    keys = {method.lower() + "_"}
    test_id = TEST_ID.match(description)
    if test_id:
        keys.add(test_id.group(1).lower() + "_")
    return [s for s in screenshots if any(s.rsplit("/", 1)[-1].lower().startswith(key) for key in keys)]


def list_screenshots(run_dir):
    folder = run_dir / "output" / "screenshots"
    if not folder.is_dir():
        return []
    return sorted(p.relative_to(run_dir).as_posix() for p in folder.rglob("*.png"))


def framework_run_id(output):
    path = output / "allure-results" / "environment.properties"
    try:
        match = re.search(r"^Run\.id=(.+)$", path.read_text(encoding="utf-8"), re.M)
    except OSError:
        return None
    return match.group(1).strip() if match else None


def generate_allure(run_dir):
    results = run_dir / "output" / "allure-results"
    command = allure_command()
    if not command or not results.is_dir() or not any(results.glob("*-result.json")):
        return None
    report = run_dir / "allure-report"
    try:
        completed = subprocess.run(command + ["generate", str(results), "-o", str(report), "--clean", "--single-file"],
                                   cwd=ROOT, capture_output=True, stdin=subprocess.DEVNULL, timeout=180)
    except (OSError, subprocess.TimeoutExpired):
        return None
    index = report / "index.html"
    if completed.returncode != 0 or not index.is_file():
        return None
    html = index.read_text(encoding="utf-8")
    index.write_text(ANALYTICS.sub("", html), encoding="utf-8")
    return "allure-report/index.html"


def stop_run(run_id):
    with STATE_LOCK:
        run = RUNS.get(run_id)
        if run is None:
            raise BadRequest("Unknown run")
        if run["status"] == "queued":
            finish(run, "stopped", "Removed from queue")
            return run
        if run["status"] != "running":
            raise BadRequest("Run is not active")
        run["status"] = "stopping"
        run["message"] = "Stopping: killing the test process tree"
        persist(run)
        proc = PROCESSES.get(run_id)
    if proc:
        kill_tree(proc)
    return run


def delete_run(run_id):
    with STATE_LOCK:
        run = RUNS.get(run_id)
        if run is None:
            raise BadRequest("Unknown run")
        if run["status"] not in FINISHED:
            raise Conflict("Stop the run before deleting it")
        shutil.rmtree(RUNS_DIR / run_id, ignore_errors=True)
        del RUNS[run_id]


def load_history():
    RUNS_DIR.mkdir(parents=True, exist_ok=True)
    for path in sorted(RUNS_DIR.glob("*/run.json")):
        try:
            run = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if not RUN_ID.match(str(run.get("id"))) or path.parent.name != run["id"] or "tests" not in run:
            continue
        if run.get("status") not in FINISHED:
            run["status"] = "interrupted"
            run["message"] = "The dashboard stopped while this run was queued or running"
            run["finished"] = run.get("finished") or now_iso()
            run["durationSec"] = duration_seconds(run)
            write_json(path, run)
        RUNS[run["id"]] = run


def run_summary(run):
    keys = ("id", "status", "message", "created", "started", "finished", "mode", "browser", "headless", "scope",
            "scopeLabel", "demo", "progress", "allureReport", "frameworkRunId")
    results = run.get("results") or {}
    return {key: run.get(key) for key in keys} | {
        "durationSec": run.get("durationSec") if run["status"] in FINISHED else duration_seconds(run),
        "counts": {k: results.get(k, 0) for k in ("total", "passed", "failed", "blocked", "skipped")} if results else None,
        "summary": results.get("summary"), "screenshots": len(results.get("screenshots") or []),
    }


def run_detail(run):
    return run_summary(run) | {"tests": run["tests"], "preflight": run.get("preflight", []),
                               "failures": (run.get("results") or {}).get("failures", []),
                               "screenshots": (run.get("results") or {}).get("screenshots", []),
                               "exitCode": run.get("exitCode")}


# ----------------------------------------------------------------------------- HTTP

CONTENT_TYPES = {".png": "image/png", ".jpg": "image/jpeg", ".html": "text/html; charset=utf-8",
                 ".log": "text/plain; charset=utf-8", ".txt": "text/plain; charset=utf-8",
                 ".md": "text/plain; charset=utf-8", ".xml": "text/xml; charset=utf-8", ".json": "application/json"}


class Handler(BaseHTTPRequestHandler):
    server_version = "TestDashboard/2.0"

    def log_message(self, fmt, *args):
        if args and str(args[0]).startswith("GET /api/"):
            return  # live polling is noisy
        sys.stderr.write("%s - %s\n" % (self.log_date_time_string(), fmt % args))

    def _host_allowed(self):
        """DNS-rebinding protection: only loopback host names."""
        host = (self.headers.get("Host") or "").rsplit(":", 1)[0].lower()
        return host in ("localhost", "127.0.0.1")

    def _send(self, status, body, content_type="application/json; charset=utf-8", extra=None):
        data = body if isinstance(body, bytes) else body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        for key, value in (extra or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(data)

    def _json(self, value, status=HTTPStatus.OK):
        self._send(status, json.dumps(value, ensure_ascii=False))

    def _error(self, status, message):
        self._json({"error": message}, status)

    def _body(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length > 100_000:
            raise BadRequest("Request too large")
        try:
            return json.loads(self.rfile.read(length) or b"{}")
        except ValueError:
            raise BadRequest("Invalid JSON")

    def _run(self, run_id):
        run = RUNS.get(run_id)
        if run is None:
            raise BadRequest("Unknown run")
        return run

    def do_GET(self):
        if not self._host_allowed():
            return self._error(HTTPStatus.FORBIDDEN, "Host not allowed")
        url = urlparse(self.path)
        path = url.path
        try:
            if path in ("/", "/index.html"):
                csp = ("default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                       "script-src 'self' 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'")
                return self._send(HTTPStatus.OK, (STATIC_DIR / "index.html").read_bytes(), "text/html; charset=utf-8",
                                  {"Content-Security-Policy": csp})
            if path == "/api/catalog":
                return self._json(catalog() | {"credentials": credential_source()})
            if path == "/api/build":
                return self._json(build_status())
            if path == "/api/build/log":
                snapshot = BUILD.get("snapshot")
                log = BUILD_DIR / snapshot / "build.log" if snapshot else None
                text = log.read_text(encoding="utf-8", errors="replace")[-20000:] if log and log.is_file() else ""
                return self._send(HTTPStatus.OK, text, "text/plain; charset=utf-8")
            if path == "/api/runs":
                with STATE_LOCK:
                    runs = sorted((run_summary(r) for r in RUNS.values()), key=lambda r: r["created"], reverse=True)
                return self._json({"runs": runs})
            match = re.fullmatch(r"/api/runs/([\w\-]+)", path)
            if match:
                with STATE_LOCK:
                    return self._json(run_detail(self._run(match.group(1))))
            match = re.fullmatch(r"/api/runs/([\w\-]+)/log", path)
            if match:
                return self._log(match.group(1), parse_qs(url.query).get("offset", ["0"])[0])
            match = re.fullmatch(r"/runs/([\w\-]+)/(.+)", path)
            if match:
                return self._file(match.group(1), unquote(match.group(2)))
            return self._error(HTTPStatus.NOT_FOUND, "Not found")
        except BadRequest as error:
            return self._error(error.status, str(error))

    def do_POST(self):
        if not self._host_allowed():
            return self._error(HTTPStatus.FORBIDDEN, "Host not allowed")
        if self.headers.get("X-Dashboard") != "1":
            return self._error(HTTPStatus.FORBIDDEN, "Missing X-Dashboard header")
        path = urlparse(self.path).path
        try:
            if path == "/api/build":
                return self._json(start_build(force=True))
            if path == "/api/runs":
                return self._json(run_summary(create_run(self._body())), HTTPStatus.CREATED)
            match = re.fullmatch(r"/api/runs/([\w\-]+)/stop", path)
            if match:
                return self._json(run_summary(stop_run(match.group(1))))
            match = re.fullmatch(r"/api/runs/([\w\-]+)/delete", path)
            if match:
                delete_run(match.group(1))
                return self._json({"deleted": match.group(1)})
            return self._error(HTTPStatus.NOT_FOUND, "Not found")
        except BadRequest as error:
            return self._error(error.status, str(error))
        except RuntimeError as error:
            return self._error(HTTPStatus.INTERNAL_SERVER_ERROR, str(error))

    def _log(self, run_id, offset):
        run = self._run(run_id)
        offset = int(offset) if str(offset).isdigit() else 0
        log = RUNS_DIR / run_id / "console.log"
        chunk = b""
        if log.is_file():
            with open(log, "rb") as handle:
                handle.seek(offset)
                chunk = handle.read(256_000)
        with STATE_LOCK:
            return self._json({"offset": offset + len(chunk), "text": chunk.decode("utf-8", errors="replace"),
                               "run": run_summary(run), "tests": run["tests"]})

    def _file(self, run_id, relative):
        """Only regular files inside runner/runs/<id>/ (never run.json); anything else is 404."""
        self._run(run_id)
        base = (RUNS_DIR / run_id).resolve()
        target = (base / relative).resolve()
        if base not in target.parents or not target.is_file() or target.name == "run.json":
            return self._error(HTTPStatus.NOT_FOUND, "Not found")
        return self._send(HTTPStatus.OK, target.read_bytes(), CONTENT_TYPES.get(target.suffix.lower(), "application/octet-stream"))


# ----------------------------------------------------------------------------- startup

def acquire_lock(port):
    """One dashboard per checkout: a second start refuses (use --stop to stop the active one)."""
    if LOCK_FILE.is_file():
        try:
            other = json.loads(LOCK_FILE.read_text())
        except ValueError:
            other = {}
        if process_alive(int(other.get("pid", 0))):
            raise SystemExit(
                f"The dashboard is already running: http://{HOST}:{other.get('port')}  (pid {other.get('pid')}).\n"
                "Open that URL, or stop it first with:  python runner/server.py --stop")
    LOCK_FILE.write_text(json.dumps({"pid": os.getpid(), "port": port, "started": now_iso()}))


def stop_existing():
    if not LOCK_FILE.is_file():
        print("No dashboard is running.")
        return
    try:
        pid = int(json.loads(LOCK_FILE.read_text()).get("pid", 0))
    except ValueError:
        pid = 0
    if not process_alive(pid):
        LOCK_FILE.unlink(missing_ok=True)
        print("No dashboard is running (removed a stale lock).")
        return
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], capture_output=True)
    else:
        os.killpg(os.getpgid(pid), signal.SIGTERM)
    for _ in range(20):
        if not process_alive(pid):
            break
        time.sleep(0.25)
    LOCK_FILE.unlink(missing_ok=True)
    print(f"Stopped dashboard pid {pid}. Runs it had in progress will show as 'interrupted' on next start.")


def main():
    for stream in (sys.stdout, sys.stderr):
        stream.reconfigure(errors="replace")
    parser = argparse.ArgumentParser(description=TITLE)
    parser.add_argument("--port", type=int, default=8090, help="port on 127.0.0.1 (default 8090)")
    parser.add_argument("--open", action="store_true", help="open the dashboard in the default browser")
    parser.add_argument("--stop", action="store_true", help="stop the dashboard that is already running, then exit")
    args = parser.parse_args()
    if args.stop:
        return stop_existing()
    try:
        server = ThreadingHTTPServer((HOST, args.port), Handler)
    except OSError as error:
        raise SystemExit(f"Cannot listen on {HOST}:{args.port} ({error}). Use --port to pick another port.")
    acquire_lock(args.port)
    load_history()
    load_snapshots()
    build = start_build()
    threading.Thread(target=scheduler, daemon=True).start()
    url = f"http://{HOST}:{args.port}/"
    print(f"{TITLE}\n  URL      {url}\n  Runs     {RUNS_DIR}\n  History  {len(RUNS)} run(s)\n"
          f"  Build    {'compiling a snapshot in the background' if build['status'] == 'building' else 'snapshot ready'}\n"
          "  Ctrl+C to stop (or: python runner/server.py --stop)", flush=True)
    if args.open:
        threading.Timer(0.5, webbrowser.open, args=(url,)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        with STATE_LOCK:
            for run_id, proc in PROCESSES.items():
                kill_tree(proc)
                RUNS[run_id]["status"] = "stopping"
        LOCK_FILE.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
