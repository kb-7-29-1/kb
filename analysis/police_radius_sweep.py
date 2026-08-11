import pandas as pd

from facilities import load_facilities
from load_safe_routes import load_routes
from comparison_routes import get_or_generate
import coverage

FACILITY_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv"
SAFE_ROUTE_SHP = "../backend/src/main/resources/safety_data/안심귀갓길 경로 데이터_수정.shp"

CANDIDATE_RADII = [50.0, 75.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 700.0, 1000.0, 1500.0]


def has_police_for_radius(route_points, police_df, radius):
    origin_lat, origin_lon = route_points[0]
    route_xy = [coverage.project(lat, lon, origin_lat, origin_lon) for lat, lon in route_points]
    nearby = coverage.bbox_filter(police_df, route_points, radius + 200)
    facilities_xy = [
        coverage.project(r.latitude, r.longitude, origin_lat, origin_lon)
        for r in nearby.itertuples()
    ]
    return coverage.has_facility_near_route(route_xy, facilities_xy, radius)


def main():
    facilities = load_facilities(FACILITY_CSV)
    safe_routes = load_routes(SAFE_ROUTE_SHP)
    comparison_routes = get_or_generate(facilities, safe_routes, target_count=50)

    all_points = [(r["points"], 1) for r in safe_routes] + [(r["points"], 0) for r in comparison_routes]

    print(f"{'반경(m)':>8s} {'label1 비율':>12s} {'label0 비율':>12s} {'격차':>8s} {'상관계수':>10s}")
    for radius in CANDIDATE_RADII:
        rows = []
        for points, label in all_points:
            has_police = has_police_for_radius(points, facilities["POLICE"], radius)
            rows.append({"label": label, "has_police": int(has_police)})
        df = pd.DataFrame(rows)
        ratio1 = df[df.label == 1]["has_police"].mean()
        ratio0 = df[df.label == 0]["has_police"].mean()
        corr = df["has_police"].corr(df["label"])
        print(f"{radius:>8.0f} {ratio1:>12.3f} {ratio0:>12.3f} {ratio0-ratio1:>8.3f} {corr:>10.3f}")


if __name__ == "__main__":
    main()