#!/usr/bin/env python3
"""phases.json을 읽어 각 태스크를 claude -p로 실행하는 스크립트."""
import json, subprocess, sys, os, tempfile
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
sys.stderr.reconfigure(encoding='utf-8', errors='replace')

def run_task(prompt: str) -> int:
    with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False, encoding='utf-8') as f:
        f.write(prompt)
        tmp = f.name
    try:
        result = subprocess.run(
            f'type "{tmp}" | claude -p - --dangerously-skip-permissions',
            shell=True,
            text=True,
            encoding='utf-8'
        )
        return result.returncode
    finally:
        os.unlink(tmp)

def run_phases(phase_ids=None):
    phases_file = Path("phases.json")
    if not phases_file.exists():
        print("phases.json not found"); sys.exit(1)

    config = json.loads(phases_file.read_text(encoding="utf-8"))
    phases = config["phases"]

    if phase_ids:
        phases = [p for p in phases if p["id"] in phase_ids]

    print(f"Project: {config['project']}\nStack:   {config['stack']}\n")

    for phase in phases:
        print(f"\n{'='*60}")
        print(f"Phase {phase['id']}: {phase['name']}")
        print(f"Goal: {phase['goal']}")
        print(f"{'='*60}")

        for task in phase["tasks"]:
            print(f"\n  Task {task['id']}: {task['title']}")
            print(f"  Expected outputs: {', '.join(task.get('outputs', []))}")

            rc = run_task(task["prompt"])

            if rc != 0:
                print(f"\n  [FAILED] Task {task['id']} failed (exit {rc}). Stopping.")
                sys.exit(rc)

            print(f"  [DONE] Task {task['id']}")

    evaluate = config.get("evaluate", {})
    if evaluate.get("enabled") and evaluate.get("commands"):
        print(f"\n{'='*60}")
        print("Evaluate")
        print(f"{'='*60}")
        for cmd in evaluate["commands"]:
            print(f"\n  Running: {cmd}")
            result = subprocess.run(cmd, shell=True)
            if result.returncode != 0:
                print(f"  [FAILED] {cmd}")
                sys.exit(result.returncode)
            print(f"  [PASS] {cmd}")

    print(f"\nAll phases complete.")

if __name__ == "__main__":
    ids = [int(x) for x in sys.argv[1:]] if len(sys.argv) > 1 else None
    run_phases(ids)
