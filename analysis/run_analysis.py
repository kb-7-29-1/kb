import pandas as pd

from facilities import load_facilities
from load_safe_routes import load_routes
from comparison_routes import get_or_generate
from coverage import compute_breakdown

FACILITY_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv"
SAFE_ROUTE_SHP = "../backend/src/main/resources/safety_data/안심귀갓길 경로 데이터_수정.shp"
FEATURES_CSV = "output/features.csv"
REPORT_TXT = "output/report.txt"

FEATURE_COLUMNS = [
    "distance_m",
    "cctv_count",
    "street_light_count",
    "cctv_coverage_pct",
    "street_light_coverage_pct",
    "has_police_station",
    "nearest_police_distance_m",
    "cctv_average_gap_m",
    "street_light_average_gap_m",
    "safety_score",
]


def build_feature_table(facilities, safe_routes, comparison_routes):
    rows = []
    for r in safe_routes:
        b = compute_breakdown(r["points"], facilities["CCTV"], facilities["STREET_LIGHT"], facilities["POLICE"])
        rows.append({"route_id": r["route_id"], "label": 1, "gu": r["gu"], **b})

    for r in comparison_routes:
        b = compute_breakdown(
            r["points"], facilities["CCTV"], facilities["STREET_LIGHT"], facilities["POLICE"],
            route_distance_m=r.get("distance_m"),
        )
        rows.append({"route_id": r["route_id"], "label": 0, "gu": None, **b})

    return pd.DataFrame(rows)


def analyze(df):
    lines = []
    lines.append(f"label=1 (안심귀갓길) 개수: {(df.label == 1).sum()}")
    lines.append(f"label=0 (비교군) 개수: {(df.label == 0).sum()}")
    lines.append("")

    lines.append("=== 그룹별 평균 비교 ===")
    numeric_cols = [c for c in FEATURE_COLUMNS if c != "has_police_station"]
    group_means = df.groupby("label")[numeric_cols].mean().round(2)
    lines.append(group_means.to_string())
    lines.append("")

    lines.append("=== 파출소 존재 비율 ===")
    police_ratio = df.groupby("label")["has_police_station"].mean().round(3)
    lines.append(police_ratio.to_string())
    lines.append("")

    lines.append("=== label과의 상관계수 (point-biserial) ===")
    corr_df = df.copy()
    corr_df["has_police_station"] = corr_df["has_police_station"].astype(int)
    corr_cols = numeric_cols + ["has_police_station"]
    correlations = corr_df[corr_cols + ["label"]].corr()["label"].drop("label").sort_values(
        key=abs, ascending=False
    )
    lines.append(correlations.round(3).to_string())
    lines.append("")

    try:
        from sklearn.linear_model import LogisticRegression
        from sklearn.preprocessing import StandardScaler

        X_cols = ["cctv_coverage_pct", "street_light_coverage_pct", "has_police_station"]
        X = corr_df[X_cols].copy()
        X["has_police_station"] = X["has_police_station"].astype(int)
        y = corr_df["label"]

        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
        model = LogisticRegression().fit(X_scaled, y)

        lines.append("=== 로지스틱 회귀 계수 (표준화된 입력 기준) ===")
        for col, coef in zip(X_cols, model.coef_[0]):
            lines.append(f"{col}: {coef:.3f}")
        lines.append(f"accuracy(전체 데이터 기준): {model.score(X_scaled, y):.3f}")
        lines.append("")
    except ImportError:
        lines.append("scikit-learn 미설치로 로지스틱 회귀는 생략됨")
        lines.append("")

    return "\n".join(lines)


def main():
    print("시설 데이터 로딩...")
    facilities = load_facilities(FACILITY_CSV)

    print("안심귀갓길 경로 로딩...")
    safe_routes = load_routes(SAFE_ROUTE_SHP)

    print("비교군 경로 로딩...")
    comparison_routes = get_or_generate(facilities, safe_routes, target_count=50)

    print("feature 추출 중...")
    df = build_feature_table(facilities, safe_routes, comparison_routes)
    df.to_csv(FEATURES_CSV, index=False)
    print(f"저장: {FEATURES_CSV} ({len(df)}행)")

    report = analyze(df)
    with open(REPORT_TXT, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"저장: {REPORT_TXT}")
    print()
    print(report)


if __name__ == "__main__":
    main()