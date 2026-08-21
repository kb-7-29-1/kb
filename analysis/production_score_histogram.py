"""
운영 DB(property_safety) 전체 export에서 destination별 안전점수 분포를 재구성한다.

DB에는 최종 safety_score만 저장돼 있고 총감점(totalPenalty)은 남아있지 않으므로,
현재 공식 score = round(100 - totalPenalty/3.0) 을 역산해 totalPenalty를 복원한 뒤
damping 시나리오별(없음 / +20 보너스 / 현재 ÷3)로 재계산한다.

score < 67 인 행은 2026-08-19 이전 구형 공식(÷2, ÷1.5 등)으로 계산된 채
재계산되지 않은 stale 캐시이므로 역산 대상에서 제외한다.
"""

import csv
from collections import defaultdict

INPUT_CSV = "output/property_safety_full_export.csv"
STALE_SCORE_THRESHOLD = 67
BIN_LABELS = ["0-9", "10-19", "20-29", "30-39", "40-49", "50-59", "60-69", "70-79", "80-89", "90-99", "100"]

FORMULAS = {
    "none": lambda penalty: max(0, 100 - penalty),
    "plus20": lambda penalty: min(100, max(0, (100 - penalty) + 20)),
    "current": lambda penalty: max(0, round(100 - penalty / 3.0)),
}


def load_penalties_by_destination(csv_path):
    penalties_by_destination = defaultdict(list)
    with open(csv_path, encoding="utf-8") as f:
        for row in csv.DictReader(f):
            score = int(row["safety_score"])
            if score < STALE_SCORE_THRESHOLD:
                continue
            penalty = 3 * (100 - score)
            penalties_by_destination[row["destination_id"]].append(penalty)
    return penalties_by_destination


def histogram(penalties, formula):
    bins = [0] * len(BIN_LABELS)
    for penalty in penalties:
        score = formula(penalty)
        bins[min(10, max(0, score) // 10)] += 1
    return bins


def grade_distribution(penalties, formula):
    counts = {"DANGER": 0, "WARNING": 0, "SAFE": 0}
    for penalty in penalties:
        score = formula(penalty)
        if score >= 80:
            counts["SAFE"] += 1
        elif score >= 60:
            counts["WARNING"] += 1
        else:
            counts["DANGER"] += 1
    return counts


DESTINATION_NAMES = {
    "1": "세종대학교",
    "5": "연세대학교",
    "126": "중앙대학교",
    "1234": "공릉역",
}


def main():
    penalties_by_destination = load_penalties_by_destination(INPUT_CSV)

    for destination_id, name in DESTINATION_NAMES.items():
        penalties = penalties_by_destination[destination_id]
        print(f"\n=== {name} (destination_id={destination_id}, n={len(penalties)}) ===")
        for scenario, formula in FORMULAS.items():
            bins = histogram(penalties, formula)
            grades = grade_distribution(penalties, formula)
            total = len(penalties)
            print(f"  [{scenario}] bins(10점단위)={bins}")
            print(
                f"  [{scenario}] DANGER={grades['DANGER']/total*100:.1f}% "
                f"WARNING={grades['WARNING']/total*100:.1f}% "
                f"SAFE={grades['SAFE']/total*100:.1f}%"
            )


if __name__ == "__main__":
    main()
