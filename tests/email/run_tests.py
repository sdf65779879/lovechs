"""Provision an isolated Docker stack and run the real HTTP/SMTP integration suite."""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "backend" / "target" / "email-integration"
ENV_FILE = OUTPUT / "test.env"
COMPOSE = ROOT / "tests" / "email" / "compose.yml"
PROJECT = "couple-email-tests"


def compose(*args, input_text=None, check=True):
    return subprocess.run(
        ["docker", "compose", "--project-name", PROJECT, "--env-file", str(ENV_FILE), "-f", str(COMPOSE), *args],
        input=input_text, text=True, encoding="utf-8", errors="replace", capture_output=True, check=check,
    )


def ensure_isolated():
    data = compose("config", "--format", "json").stdout
    config = json.loads(data)
    if config.get("name") != PROJECT:
        raise RuntimeError("Refusing to operate on a non-test Docker project")
    if config["services"]["backend"]["environment"]["MYSQL_PASSWORD"] != "fixture-db-password":
        raise RuntimeError("Refusing to operate with non-fixture database credentials")


def sql(query):
    ensure_isolated()
    return compose("exec", "-T", "mysql", "sh", "-c",
                   "MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot -N couple_ordering", input_text=query).stdout.strip()


def wait_ready(timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen("http://127.0.0.1:19080/api/v1/health", timeout=2) as response:
                if json.load(response).get("code") == 0:
                    return
        except (OSError, ValueError):
            pass
        time.sleep(1)
    raise RuntimeError("Isolated backend did not become healthy")


def start(skip_build):
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if not ENV_FILE.exists():
        ENV_FILE.write_text("TEST_JWT_SECRET=" + secrets.token_hex(32) + "\nTEST_MAIL_KEY="
                            + base64.b64encode(os.urandom(32)).decode() + "\n", encoding="utf-8")
    ensure_isolated()
    if not skip_build:
        with (OUTPUT / "build.log").open("w", encoding="utf-8") as log:
            subprocess.run(["docker", "compose", "-f", str(ROOT / "docker-compose.yml"), "build", "backend", "frontend"],
                           cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
    print("Starting isolated MySQL, SMTP inbox, backend and frontend...", flush=True)
    result = compose("up", "-d", "--wait", "--wait-timeout", "120")
    (OUTPUT / "start.log").write_text(result.stdout + result.stderr, encoding="utf-8")
    wait_ready()
    print("Ready: http://127.0.0.1:19080 ; SMTP inbox: http://127.0.0.1:19026/messages", flush=True)


def stop():
    if ENV_FILE.exists():
        ensure_isolated()
        compose("down", "--volumes", "--remove-orphans")
        print("Isolated test stack and its volumes removed.", flush=True)


def parse_e2e_result(text):
    normalized = text.replace("\r\n", "\n")
    if "### Result\n" not in normalized:
        raise RuntimeError("Playwright did not return an E2E result")
    value, _ = json.JSONDecoder().raw_decode(normalized.split("### Result\n", 1)[1].lstrip())
    if not isinstance(value, dict) or value.get("result") != "PASS":
        raise RuntimeError("Playwright returned a failed E2E result")
    return value


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("start", "integration", "e2e", "all", "stop"))
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--keep-environment", action="store_true")
    args = parser.parse_args()
    if args.action == "stop":
        stop()
        return
    try:
        start(args.skip_build)
        if args.action in ("integration", "all"):
            result = subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", str(ROOT / "tests" / "email"),
                                     "-p", "integration_test.py", "-v"], cwd=ROOT)
            logs = compose("logs", "--no-color", "backend").stdout
            (OUTPUT / "backend.log").write_text(logs, encoding="utf-8")
            if "fixture-app-password" in logs or "wrong-fixture-password" in logs:
                raise RuntimeError("SMTP password leaked into backend logs")
            if result.returncode:
                raise RuntimeError("Integration suite failed; see backend/target/email-integration")
        if args.action in ("e2e", "all"):
            (ROOT / "output" / "playwright").mkdir(parents=True, exist_ok=True)
            executable = shutil.which("npx.cmd" if os.name == "nt" else "npx")
            if not executable:
                raise RuntimeError("E2E requires Node.js and npx")
            command = [executable, "--yes", "--package", "@playwright/cli", "playwright-cli", "-s=email-e2e-tests"]
            try:
                subprocess.run(command + ["open", "http://127.0.0.1:19080/admin/login"], cwd=ROOT, check=True,
                               stdout=subprocess.DEVNULL)
                subprocess.run(command + ["snapshot"], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
                result = subprocess.run(command + ["run-code", "--filename", str(ROOT / "tests" / "email" / "e2e.js")],
                                        cwd=ROOT, text=True, encoding="utf-8", errors="replace", capture_output=True)
                (OUTPUT / "e2e-result.txt").write_text(result.stdout + result.stderr, encoding="utf-8")
                if result.returncode:
                    raise RuntimeError("E2E suite failed; see backend/target/email-integration/e2e-result.txt")
                parsed = parse_e2e_result(result.stdout)
                (OUTPUT / "e2e-result.json").write_text(json.dumps(parsed, ensure_ascii=False, indent=2), encoding="utf-8")
                print(json.dumps(parsed, ensure_ascii=False, indent=2), flush=True)
            finally:
                subprocess.run(command + ["close"], cwd=ROOT, stdout=subprocess.DEVNULL, check=False)
    finally:
        if args.action in ("integration", "e2e", "all") and not args.keep_environment:
            stop()


if __name__ == "__main__":
    main()
