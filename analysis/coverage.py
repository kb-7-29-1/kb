import math

EARTH_RADIUS_M = 6_371_000.0

CCTV_ROUTE_RADIUS_METERS = 50.0
STREET_LIGHT_ROUTE_RADIUS_METERS = 30.0
POLICE_ROUTE_RADIUS_METERS = 300.0
CCTV_SECTION_METERS = 50.0
STREET_LIGHT_SECTION_METERS = 30.0


def project(lat, lon, origin_lat, origin_lon):
    x = math.radians(lon - origin_lon) * EARTH_RADIUS_M * math.cos(math.radians((lat + origin_lat) / 2.0))
    y = math.radians(lat - origin_lat) * EARTH_RADIUS_M
    return x, y


def bbox_filter(facilities_df, route_points, margin_m):
    lats = [p[0] for p in route_points]
    lons = [p[1] for p in route_points]
    min_lat, max_lat = min(lats), max(lats)
    min_lon, max_lon = min(lons), max(lons)
    mid_lat = (min_lat + max_lat) / 2.0
    lat_margin = margin_m / 111_000.0
    lon_margin = margin_m / (111_000.0 * math.cos(math.radians(mid_lat)) + 1e-9)
    mask = (
        (facilities_df["latitude"] >= min_lat - lat_margin)
        & (facilities_df["latitude"] <= max_lat + lat_margin)
        & (facilities_df["longitude"] >= min_lon - lon_margin)
        & (facilities_df["longitude"] <= max_lon + lon_margin)
    )
    return facilities_df[mask]


def polyline_length(points_xy):
    total = 0.0
    for i in range(1, len(points_xy)):
        total += math.dist(points_xy[i - 1], points_xy[i])
    return total


def point_at_distance(points_xy, target_distance):
    if target_distance <= 0.0:
        return points_xy[0]
    traversed = 0.0
    for i in range(1, len(points_xy)):
        start = points_xy[i - 1]
        end = points_xy[i]
        segment_length = math.dist(start, end)
        if segment_length == 0.0:
            continue
        if traversed + segment_length >= target_distance:
            ratio = (target_distance - traversed) / segment_length
            return (
                start[0] + (end[0] - start[0]) * ratio,
                start[1] + (end[1] - start[1]) * ratio,
            )
        traversed += segment_length
    return points_xy[-1]


def distance_point_to_segment(point, start, end):
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    denominator = dx * dx + dy * dy
    if denominator == 0.0:
        return math.dist(point, start)
    t = ((point[0] - start[0]) * dx + (point[1] - start[1]) * dy) / denominator
    t = max(0.0, min(1.0, t))
    closest_x = start[0] + t * dx
    closest_y = start[1] + t * dy
    return math.hypot(point[0] - closest_x, point[1] - closest_y)


def distance_point_to_polyline(point, polyline_xy):
    minimum = math.inf
    for i in range(1, len(polyline_xy)):
        minimum = min(minimum, distance_point_to_segment(point, polyline_xy[i - 1], polyline_xy[i]))
    return minimum


def calculate_coverage(route_xy, facilities_xy, section_length, radius):
    total_length = polyline_length(route_xy)
    if total_length <= 0.0 or len(facilities_xy) == 0:
        return 0.0
    section_count = max(1, math.ceil(total_length / section_length))
    covered = 0
    radius_sq = radius * radius
    for index in range(section_count):
        target_distance = min(total_length, (index + 0.5) * section_length)
        sample = point_at_distance(route_xy, target_distance)
        for fx, fy in facilities_xy:
            dx = sample[0] - fx
            dy = sample[1] - fy
            if dx * dx + dy * dy <= radius_sq:
                covered += 1
                break
    return covered * 100.0 / section_count


def count_near_route(route_xy, facilities_xy_count, radius):
    total = 0
    for fx, fy, count in facilities_xy_count:
        if distance_point_to_polyline((fx, fy), route_xy) <= radius:
            total += max(1, count)
    return total


def has_facility_near_route(route_xy, facilities_xy, radius):
    for fx, fy in facilities_xy:
        if distance_point_to_polyline((fx, fy), route_xy) <= radius:
            return True
    return False


def nearest_facility_distance(route_xy, facilities_xy):
    if not facilities_xy:
        return math.inf
    return min(distance_point_to_polyline((fx, fy), route_xy) for fx, fy in facilities_xy)


def calculate_cctv_density_penalty(average_gap_m):
    if average_gap_m <= 50.0:
        return 0
    if average_gap_m <= 75.0:
        return 5
    if average_gap_m <= 100.0:
        return 10
    if average_gap_m <= 150.0:
        return 15
    return 20


def calculate_cctv_coverage_penalty(coverage_pct):
    if coverage_pct >= 80.0:
        return 0
    if coverage_pct >= 60.0:
        return 4
    if coverage_pct >= 40.0:
        return 8
    if coverage_pct >= 20.0:
        return 12
    return 15


def calculate_street_light_coverage_penalty(coverage_pct):
    if coverage_pct >= 80.0:
        return 0
    if coverage_pct >= 60.0:
        return 10
    if coverage_pct >= 40.0:
        return 20
    if coverage_pct >= 20.0:
        return 30
    return 45


def compute_breakdown(route_points, cctv_df, street_light_df, police_df, route_distance_m=None):
    origin_lat, origin_lon = route_points[0]
    route_xy = [project(lat, lon, origin_lat, origin_lon) for lat, lon in route_points]

    cctv_nearby = bbox_filter(cctv_df, route_points, CCTV_ROUTE_RADIUS_METERS + 200)
    street_light_nearby = bbox_filter(street_light_df, route_points, STREET_LIGHT_ROUTE_RADIUS_METERS + 200)
    police_nearby = bbox_filter(police_df, route_points, POLICE_ROUTE_RADIUS_METERS + 200)

    cctv_xy_count = [
        (*project(r.latitude, r.longitude, origin_lat, origin_lon), r.facility_count)
        for r in cctv_nearby.itertuples()
    ]
    street_light_xy_count = [
        (*project(r.latitude, r.longitude, origin_lat, origin_lon), r.facility_count)
        for r in street_light_nearby.itertuples()
    ]
    police_xy = [
        project(r.latitude, r.longitude, origin_lat, origin_lon)
        for r in police_nearby.itertuples()
    ]

    cctv_xy = [(x, y) for x, y, _ in cctv_xy_count]
    street_light_xy = [(x, y) for x, y, _ in street_light_xy_count]

    cctv_count = count_near_route(route_xy, cctv_xy_count, CCTV_ROUTE_RADIUS_METERS)
    street_light_count = count_near_route(route_xy, street_light_xy_count, STREET_LIGHT_ROUTE_RADIUS_METERS)
    cctv_coverage = calculate_coverage(route_xy, cctv_xy, CCTV_SECTION_METERS, CCTV_ROUTE_RADIUS_METERS)
    street_light_coverage = calculate_coverage(
        route_xy, street_light_xy, STREET_LIGHT_SECTION_METERS, STREET_LIGHT_ROUTE_RADIUS_METERS
    )
    has_police = has_facility_near_route(route_xy, police_xy, POLICE_ROUTE_RADIUS_METERS)
    nearest_police_distance = nearest_facility_distance(route_xy, police_xy)

    route_distance = route_distance_m if route_distance_m else polyline_length(route_xy)
    average_gap = math.inf if cctv_count == 0 else route_distance / cctv_count
    street_light_average_gap = math.inf if street_light_count == 0 else route_distance / street_light_count

    cctv_density_penalty = calculate_cctv_density_penalty(average_gap)
    cctv_coverage_penalty = calculate_cctv_coverage_penalty(cctv_coverage)
    street_light_penalty = calculate_street_light_coverage_penalty(street_light_coverage)
    police_penalty = 0 if has_police else 20
    total_penalty = cctv_density_penalty + cctv_coverage_penalty + street_light_penalty + police_penalty
    safety_score = max(0, round(100 - total_penalty / 2.0))

    return {
        "distance_m": round(route_distance, 1),
        "cctv_count": cctv_count,
        "street_light_count": street_light_count,
        "cctv_coverage_pct": round(cctv_coverage, 1),
        "street_light_coverage_pct": round(street_light_coverage, 1),
        "has_police_station": has_police,
        "nearest_police_distance_m": None if math.isinf(nearest_police_distance) else round(nearest_police_distance, 1),
        "cctv_average_gap_m": None if math.isinf(average_gap) else round(average_gap, 1),
        "street_light_average_gap_m": None if math.isinf(street_light_average_gap) else round(street_light_average_gap, 1),
        "cctv_density_penalty": cctv_density_penalty,
        "cctv_coverage_penalty": cctv_coverage_penalty,
        "street_light_penalty": street_light_penalty,
        "police_penalty": police_penalty,
        "total_penalty": total_penalty,
        "safety_score": safety_score,
    }