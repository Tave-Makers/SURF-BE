#!/usr/bin/env python3
"""금칙어 사전 빌드 스크립트 (docs/moderation-plan.md §1.3)

원본 두 소스를 내려받아 병합하고 exclude.txt를 적용해
src/main/resources/moderation/badwords.txt 를 재생성한다.

사용법:
    python3 scripts/build-badwords.py

원본이 업데이트되면 이 스크립트를 다시 돌리면 된다 — 큐레이션 판단은
scripts/exclude.txt 에 보존되어 있어 재생성해도 잃지 않는다.
"""

import sys
import urllib.request
from pathlib import Path

VANE_URL = "https://raw.githubusercontent.com/VaneProject/bad-word-filtering/master/badwords.txt"
LDNOOBW_URL = "https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/ko"

SCRIPT_DIR = Path(__file__).resolve().parent
EXCLUDE_PATH = SCRIPT_DIR / "exclude.txt"
OUTPUT_PATH = SCRIPT_DIR.parent / "src/main/resources/moderation/badwords.txt"

# 병합 결과가 이보다 적으면 원본이 손상됐다고 보고 중단한다.
# 현재 병합 규모는 2천 건대이므로 500은 충분히 보수적인 하한이다.
MIN_MERGED = 500


def fetch(url: str) -> str:
    with urllib.request.urlopen(url, timeout=30) as res:
        return res.read().decode("utf-8")


def parse_lines(text: str) -> list[str]:
    """1줄 1단어 형식 파싱 — '#' 이후 주석 제거, 공백 줄 무시."""
    words = []
    for line in text.splitlines():
        word = line.split("#", 1)[0].strip()
        if word:
            words.append(word)
    return words


def main() -> int:
    vane = [w.strip() for w in fetch(VANE_URL).split(",") if w.strip()]
    ldnoobw = parse_lines(fetch(LDNOOBW_URL))
    merged = set(vane) | set(ldnoobw)

    excludes = parse_lines(EXCLUDE_PATH.read_text(encoding="utf-8"))
    stale = [w for w in excludes if w not in merged]
    if stale:
        # 원본에서 사라진 제외 항목 — 오류는 아니지만 큐레이션 재검토 신호
        print(f"[경고] 원본에 없는 제외 항목 {len(stale)}개: {', '.join(stale)}")

    result = sorted(merged - set(excludes))

    # 빈 사전을 배포하면 기동 시 ModerationDictionaryEmptyException 으로 서비스가 죽는다.
    # 기존 산출물을 덮어쓰기 전에 검증하고, 실패하면 파일을 보존한 채 비정상 종료한다.
    errors = []
    if not vane:
        errors.append(f"VaneProject 원본이 비어 있습니다 ({VANE_URL})")
    if not ldnoobw:
        errors.append(f"LDNOOBW 원본이 비어 있습니다 ({LDNOOBW_URL})")
    if len(merged) < MIN_MERGED:
        errors.append(f"병합 결과가 비정상적으로 적습니다 — {len(merged)}건 (최소 {MIN_MERGED}건)")
    if not result:
        errors.append("제외 적용 후 최종 사전이 비어 있습니다")
    if errors:
        for error in errors:
            print(f"[오류] {error}", file=sys.stderr)
        print(f"[오류] {OUTPUT_PATH} 를 덮어쓰지 않고 중단합니다.", file=sys.stderr)
        return 1

    header = (
        "# 자동 생성 파일 — 직접 수정 금지. scripts/build-badwords.py 로 재생성한다.\n"
        "# 원본: VaneProject bad-word-filtering badwords.txt (MIT)\n"
        "#       + LDNOOBW ko (CC BY 4.0)\n"
        "# 라이선스 고지: 같은 디렉터리의 NOTICE.md\n"
        f"# 병합 {len(merged)} - 제외 {len(excludes)} = {len(result)}개 (제외 사유: scripts/exclude.txt)\n"
    )
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(header + "\n".join(result) + "\n", encoding="utf-8")

    print(f"원본: VaneProject {len(vane)} + LDNOOBW {len(ldnoobw)} → 병합 {len(merged)}")
    print(f"제외 {len(excludes)}개 적용 → 최종 {len(result)}개")
    print(f"출력: {OUTPUT_PATH.relative_to(SCRIPT_DIR.parent)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
