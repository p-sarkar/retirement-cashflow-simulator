#!/usr/bin/env python3
"""
Report outdated and vulnerable dependencies across the project's tiers, read-only.

  python3 .claude/skills/upgrade-dependencies/scripts/dependency_report.py [--repo PATH] [--json]

Tiers: API server (Gradle: dependencies, plugins, wrapper), BFF (Deno: jsr/npm imports), frontend (npm),
GitHub Actions (CI workflows). Uses only the Python standard library plus the project's own tools
(npm, deno) and public endpoints (Maven Central, services.gradle.org, jsr.io, api.osv.dev, GitHub).
Nothing is installed or changed. Lookups that fail are reported as "?" rather than stopping the report.
"""
import argparse
import json
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

TIMEOUT = 20
PRERELEASE = re.compile(r"(alpha|beta|rc|cr|m\d|milestone|eap|dev|snapshot|preview|pre|-b\d|canary|next)", re.I)


def http_json(url, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "User-Agent": "dependency-report"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return json.load(resp)


def http_text(url):
    req = urllib.request.Request(url, headers={"User-Agent": "dependency-report"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return resp.read().decode()


def version_key(v):
    """Numeric parts for ordering stable versions: 3.10.1 > 3.9.0; non-numeric parts sort low."""
    return [int(p) if p.isdigit() else -1 for p in re.split(r"[.\-+]", v.lstrip("v"))]


def update_type(current, latest):
    if not current or not latest or latest in ("?", current):
        return "" if latest == current else "?"
    c, l = version_key(current), version_key(latest)
    if l <= c:
        return ""
    # For 0.x versions, a minor bump is breaking (semver), so call it major
    if c[0] != l[0] or (c[0] == 0 and len(c) > 1 and len(l) > 1 and c[1] != l[1]):
        return "MAJOR"
    if len(c) > 1 and len(l) > 1 and c[1] != l[1]:
        return "minor"
    return "patch"


def run(cmd, cwd):
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=300)
        return p.returncode, p.stdout, p.stderr
    except (OSError, subprocess.TimeoutExpired) as e:
        return -1, "", str(e)


# --- Maven / Gradle -------------------------------------------------------------------------------------------

def maven_latest_stable(group, artifact):
    try:
        xml = http_text(f"https://repo1.maven.org/maven2/{group.replace('.', '/')}/{artifact}/maven-metadata.xml")
    except Exception:
        return "?"
    versions = [v for v in re.findall(r"<version>([^<]+)</version>", xml) if not PRERELEASE.search(v)]
    return max(versions, key=version_key) if versions else "?"


def osv_vulns(ecosystem, name, version):
    try:
        data = http_json("https://api.osv.dev/v1/query", {"package": {"name": name, "ecosystem": ecosystem}, "version": version})
    except Exception:
        return ["?"]
    return sorted({v["id"] for v in data.get("vulns", [])})


def gradle_report(repo):
    api = repo / "api-server"
    props = {}
    for line in (api / "gradle.properties").read_text().splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    build = (api / "build.gradle.kts").read_text()

    rows, seen = [], set()
    # implementation("group:artifact:$var") or literal versions; one row per (group, version variable)
    for group, artifact, ver in re.findall(r'"([\w.\-]+):([\w.\-]+):([^"]+)"', build):
        var = ver[1:] if ver.startswith("$") else None
        current = props.get(var, "?") if var else ver
        key = (group, var or ver)
        if key in seen:
            continue
        seen.add(key)
        latest = maven_latest_stable(group, artifact)
        rows.append({
            "name": f"{group}:{artifact}" + (f" (all {group} via {var})" if var else ""),
            "declared_in": f"gradle.properties {var}" if var else "build.gradle.kts",
            "current": current, "latest": latest, "update": update_type(current, latest),
            "vulns": osv_vulns("Maven", f"{group}:{artifact}", current) if current != "?" else [],
        })

    # Plugins: kotlin("jvm") version "X" -> Kotlin Gradle plugin; id("x") version "y"
    for plugin, ver in re.findall(r'kotlin\("([\w.\-]+)"\)\s+version\s+"([^"]+)"', build):
        latest = maven_latest_stable("org.jetbrains.kotlin", "kotlin-gradle-plugin")
        rows.append({"name": f"Kotlin plugin {plugin}", "declared_in": "build.gradle.kts plugins (keep gradle.properties kotlinVersion in step)",
                     "current": ver, "latest": latest, "update": update_type(ver, latest), "vulns": []})
    for plugin_id, ver in re.findall(r'id\("([\w.\-]+)"\)\s+version\s+"([^"]+)"', build):
        latest = maven_latest_stable(plugin_id, f"{plugin_id}.gradle.plugin")
        rows.append({"name": f"plugin {plugin_id}", "declared_in": "build.gradle.kts plugins",
                     "current": ver, "latest": latest, "update": update_type(ver, latest), "vulns": []})

    wrapper = (api / "gradle/wrapper/gradle-wrapper.properties").read_text()
    m = re.search(r"gradle-([\d.]+)-(bin|all)\.zip", wrapper)
    current = m.group(1) if m else "?"
    try:
        latest = http_json("https://services.gradle.org/versions/current")["version"]
    except Exception:
        latest = "?"
    rows.append({"name": "Gradle wrapper", "declared_in": "gradle/wrapper/gradle-wrapper.properties",
                 "current": current, "latest": latest, "update": update_type(current, latest), "vulns": []})
    return rows


# --- Deno -----------------------------------------------------------------------------------------------------

def deno_report(repo):
    backend = repo / "backend"
    imports = json.loads((backend / "deno.json").read_text()).get("imports", {})
    rows = []
    for alias, spec in imports.items():
        m = re.match(r"(jsr|npm):(@?[^@]+)@(.+)", spec)
        if not m:
            continue
        registry, name, wanted = m.groups()
        latest, vulns = "?", []
        try:
            if registry == "jsr":
                latest = http_json(f"https://jsr.io/{name}/meta.json")["latest"]
            else:
                latest = http_json(f"https://registry.npmjs.org/{name}/latest")["version"]
        except Exception:
            pass
        current = re.sub(r"^[\^~]", "", wanted)
        if registry == "npm" and re.match(r"^\d+\.\d+\.\d+$", current):
            vulns = osv_vulns("npm", name, current)
        rows.append({"name": f"{registry}:{name}", "declared_in": "backend/deno.json",
                     "current": wanted, "latest": latest, "update": update_type(current, latest), "vulns": vulns})
    code, out, err = run(["deno", "outdated"], backend)
    note = out.strip() or err.strip() or "deno outdated: everything up to date within the declared ranges"
    return rows, note


# --- npm ------------------------------------------------------------------------------------------------------

def npm_report(repo):
    frontend = repo / "frontend"
    _, out, _ = run(["npm", "outdated", "--json"], frontend)  # exits 1 when anything is outdated
    try:
        outdated = json.loads(out or "{}")
    except json.JSONDecodeError:
        outdated = {}
    rows = [{"name": name, "declared_in": "frontend/package.json", "current": info.get("current", "?"),
             "wanted": info.get("wanted", "?"), "latest": info.get("latest", "?"),
             "update": update_type(info.get("current", ""), info.get("latest", "")), "vulns": []}
            for name, info in sorted(outdated.items())]

    _, out, _ = run(["npm", "audit", "--json"], frontend)
    try:
        audit = json.loads(out or "{}")
    except json.JSONDecodeError:
        audit = {}
    advisories = []
    for name, v in sorted(audit.get("vulnerabilities", {}).items()):
        fix = v.get("fixAvailable")
        fix_text = ("yes" if fix is True else f"via {fix.get('name')}@{fix.get('version')}" + (" (MAJOR)" if fix.get("isSemVerMajor") else "")
                    if isinstance(fix, dict) else "no")
        advisories.append({"package": name, "severity": v.get("severity"), "direct": v.get("isDirect"),
                           "range": v.get("range"), "fix": fix_text,
                           "via": sorted({x if isinstance(x, str) else x.get("title", "") for x in v.get("via", [])})})
    return rows, advisories, audit.get("metadata", {}).get("vulnerabilities", {})


# --- GitHub Actions -------------------------------------------------------------------------------------------

def actions_report(repo):
    rows, seen = [], set()
    for wf in sorted((repo / ".github/workflows").glob("*.y*ml")):
        for action, ref in re.findall(r"uses:\s*([\w.\-]+/[\w.\-]+)(?:/[\w.\-/]+)?@([\w.\-]+)", wf.read_text()):
            if (action, ref) in seen:
                continue
            seen.add((action, ref))
            latest = "?"
            code, out, _ = run(["gh", "api", f"repos/{action}/releases/latest", "--jq", ".tag_name"], repo)
            if code == 0 and out.strip():
                latest = out.strip()
            else:
                try:
                    latest = http_json(f"https://api.github.com/repos/{action}/releases/latest")["tag_name"]
                except Exception:
                    pass
            # Workflows pin major tags (v4); compare majors
            cur_major = re.match(r"v?(\d+)", ref)
            lat_major = re.match(r"v?(\d+)", latest)
            upd = "MAJOR" if cur_major and lat_major and int(lat_major.group(1)) > int(cur_major.group(1)) else ""
            rows.append({"name": action, "declared_in": wf.name, "current": ref, "latest": latest, "update": upd, "vulns": []})
    return rows


# --- Output ---------------------------------------------------------------------------------------------------

def table(rows, wanted=False):
    head = "| Package | Current | " + ("Wanted | " if wanted else "") + "Latest stable | Update | Known vulnerabilities | Declared in |"
    sep = "|---|---|" + ("---|" if wanted else "") + "---|---|---|---|"
    lines = [head, sep]
    for r in rows:
        vul = ", ".join(r["vulns"]) if r["vulns"] else "none"
        lines.append(f"| {r['name']} | {r['current']} | " + (f"{r.get('wanted', '')} | " if wanted else "")
                     + f"{r['latest']} | {r['update'] or 'up to date'} | {vul} | {r['declared_in']} |")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", default=".", help="repository root (default: current directory)")
    ap.add_argument("--json", action="store_true", help="print JSON instead of Markdown")
    args = ap.parse_args()
    repo = Path(args.repo).resolve()

    gradle = gradle_report(repo)
    deno, deno_note = deno_report(repo)
    npm, advisories, npm_counts = npm_report(repo)
    actions = actions_report(repo)

    if args.json:
        json.dump({"gradle": gradle, "deno": {"imports": deno, "deno_outdated": deno_note}, "npm": {"outdated": npm,
                   "audit": advisories, "audit_counts": npm_counts}, "github_actions": actions}, sys.stdout, indent=2)
        return

    print("# Dependency report\n")
    print("## API server (Gradle)\n")
    print(table(gradle))
    print("\n## BFF (Deno)\n")
    print(table(deno))
    print(f"\n`deno outdated`:\n\n```\n{deno_note}\n```")
    print("\n## Frontend (npm)\n")
    print(table(npm, wanted=True) if npm else "Everything up to date.")
    print("\n### npm audit\n")
    if advisories:
        counts = ", ".join(f"{n} {k}" for k, n in npm_counts.items() if n and k != "total")
        print(f"{npm_counts.get('total', len(advisories))} vulnerable packages ({counts}).\n")
        print("| Package | Severity | Direct | Vulnerable range | Fix | Advisories |\n|---|---|---|---|---|---|")
        for a in advisories:
            via = a["via"] if len(a["via"]) <= 2 else a["via"][:2] + [f"and {len(a['via']) - 2} more"]
            print(f"| {a['package']} | {a['severity']} | {'yes' if a['direct'] else 'no'} | {a['range']} | {a['fix']} | {'; '.join(via)} |")
    else:
        print("No known vulnerabilities.")
    print("\n## GitHub Actions\n")
    print(table(actions))


if __name__ == "__main__":
    main()
