import pandas as pd

from facilities import load_facilities
from load_safe_routes import load_routes
from comparison_routes import get_or_generate
import coverage

FACILITY_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv"
SAFE_ROUTE_SHP = "../backend/src/main/resources/safety_data/안심귀갓길 경로 데이터_수정.shp"

CANDIDATE_RADII = [15.0, 20.0, 30.0, 40.0, 50.0, 70.0]


def street_light_coverage_for_radius(route_points, street_light_df, radius):
    origin_lat, origin_lon = route_points[0]
    route_xy = [coverage.project(lat, lon, origin_lat, origin_lon) for lat, lon in route_points]
    nearby = coverage.bbox_filter(street_light_df, route_points, radius + 200)
    facilities_xy = [
        coverage.project(r.latitude, r.longitude, origin_lat, origin_lon)
        for r in nearby.itertuples()
    ]
    return coverage.calculate_coverage(route_xy, facilities_xy, radius, radius)


def main():
    facilities = load_facilities(FACILITY_CSV)
    safe_routes = load_routes(SAFE_ROUTE_SHP)
    comparison_routes = get_or_generate(facilities, safe_routes, target_count=50)

    all_points = [(r["points"], 1) for r in safe_routes] + [(r["points"], 0) for r in comparison_routes]

    print(f"{'반경(m)':>8s} {'label1 평균':>12s} {'label0 평균':>12s} {'격차':>8s} {'상관계수':>10s}")
    for radius in CANDIDATE_RADII:
        rows = []
        for points, label in all_points:
            pct = street_light_coverage_for_radius(points, facilities["STREET_LIGHT"], radius)
            rows.append({"label": label, "pct": pct})
        df = pd.DataFrame(rows)
        mean1 = df[df.label == 1]["pct"].mean()
        mean0 = df[df.label == 0]["pct"].mean()
        corr = df["pct"].corr(df["label"])
        print(f"{radius:>8.0f} {mean1:>12.2f} {mean0:>12.2f} {mean0-mean1:>8.2f} {corr:>10.3f}")


if __name__ == "__main__":
    main()