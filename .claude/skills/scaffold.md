---
description: 새 프로젝트 스캐폴딩 - Clarify → Plan → Generate → Evaluate 파이프라인. 빈 코드베이스에서 아이디어를 실제 코드로 변환.
---

# /scaffold

새 프로젝트를 처음부터 구축하는 4단계 파이프라인.

## 사용법
```
/scaffold [프로젝트 설명]
```
인자가 없으면 먼저 설명을 요청한다.

---

## 파이프라인 단계

### 1단계: Clarify

사용자에게 다음 항목들에 대한 논의점을 제안하고 답변을 수집한다.

**반드시 확인할 항목:**
- **목적 & 범위**: 무엇을 만드는가? 핵심 기능 3가지는?
- **기술 스택**: 언어, 프레임워크, 런타임 선호도
- **UX / 인터페이스**: CLI / Web / API / Desktop / 모바일?
- **데이터**: 영속성 필요 여부, DB 종류 (SQLite / Postgres / 없음), 외부 API 연동
- **인증**: 로그인/권한 필요 여부
- **배포**: 로컬 전용 / 서버 / 클라우드?
- **제약**: 금지 라이브러리, 필수 언어, 성능 요구사항

사용자 답변에 따라 구현 불가능하거나 복잡도가 급증하는 조합을 경고한다.
모든 항목이 명확해지면 2단계로 진행한다.

---

### 2단계: Context Gather

빈 코드베이스이므로 **건너뛴다.**

---

### 3단계: Plan

현재 작업 디렉토리에 두 파일을 생성한다.

#### `phases.json`
```json
{
  "project": "<프로젝트명>",
  "stack": "<기술 스택 요약>",
  "phases": [
    {
      "id": 1,
      "name": "<페이즈명>",
      "goal": "<이 페이즈가 끝났을 때 달성되는 것>",
      "tasks": [
        {
          "id": "1.1",
          "title": "<태스크 제목>",
          "prompt": "<Claude에게 줄 구체적인 실행 프롬프트>",
          "outputs": ["<생성될 파일 목록>"]
        }
      ]
    }
  ],
  "evaluate": {
    "enabled": true,
    "commands": ["<빌드/검사 명령어>"]
  }
}
```

**페이즈 분할 원칙:**
- 각 페이즈는 독립적으로 검증 가능해야 한다
- Phase 1: 프로젝트 구조 + 핵심 모델/스키마
- Phase 2: 비즈니스 로직 / 핵심 기능
- Phase 3: UI / API 엔드포인트
- Phase 4: 설정, 환경변수, 실행 스크립트
- 필요 시 페이즈 추가 가능

#### `run_phases.py`
```python
#!/usr/bin/env python3
"""phases.json을 읽어 각 태스크를 claude -p로 실행하는 스크립트."""
import json, subprocess, sys, os
from pathlib import Path

def run_phases(phase_ids=None):
    phases_file = Path("phases.json")
    if not phases_file.exists():
        print("phases.json not found"); sys.exit(1)

    config = json.loads(phases_file.read_text(encoding="utf-8"))
    phases = config["phases"]

    if phase_ids:
        phases = [p for p in phases if p["id"] in phase_ids]

    print(f"Project: {config['project']} | Stack: {config['stack']}\n")

    for phase in phases:
        print(f"\n{'='*60}")
        print(f"Phase {phase['id']}: {phase['name']}")
        print(f"Goal: {phase['goal']}")
        print(f"{'='*60}")

        for task in phase["tasks"]:
            print(f"\n  Task {task['id']}: {task['title']}")
            print(f"  Expected outputs: {', '.join(task.get('outputs', []))}")

            result = subprocess.run(
                ["claude", "-p", task["prompt"]],
                capture_output=False,
                text=True
            )

            if result.returncode != 0:
                print(f"\n  [FAILED] Task {task['id']} failed. Stopping.")
                sys.exit(result.returncode)

            print(f"  [DONE] Task {task['id']}")

    # Evaluate
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
    # 인자로 phase id를 받을 수 있음: python run_phases.py 1 2
    ids = [int(x) for x in sys.argv[1:]] if len(sys.argv) > 1 else None
    run_phases(ids)
```

두 파일 생성 후 사용자에게 내용을 요약하고 **수정 여부를 확인**한다.
확인이 나오면 4단계로 진행한다.

---

### 4단계: Generate

`run_phases.py`를 실행한다.

```bash
python run_phases.py
```

특정 페이즈만 실행하려면:
```bash
python run_phases.py 1 2
```

실행 중 실패한 태스크가 있으면 원인을 분석하고 `phases.json`의 해당 prompt를 수정한 뒤 재시도한다.

---

### 5단계: Evaluate (선택)

`phases.json`의 `evaluate.commands`에 정의된 명령어를 순서대로 실행한다.

**일반적인 명령어 예시:**
- TypeScript: `npx tsc --noEmit`
- ESLint: `npx eslint src/`
- Python: `ruff check .` 또는 `mypy .`
- 빌드: `npm run build` / `go build ./...`
- 테스트: `npm test` / `pytest`

오류가 있으면 수정 후 다시 실행한다. 모두 통과하면 완료를 알린다.

---

## 출력 파일 구조

```
<작업 디렉토리>/
├── phases.json        # 계획 (수동 편집 가능)
├── run_phases.py      # 실행 스크립트
└── <생성된 프로젝트 파일들>
```
