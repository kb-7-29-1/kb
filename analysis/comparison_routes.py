import json
import math
import os
import random
import re
import time
import urllib.parse

import requests

APPLICATION_PROPERTIES = "../backend/src/main/resources/application.properties"
CACHE_PATH = "output/comparison_routes_cache.json"
PEDESTRIAN_ROUTE_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1"

MIN_PAIR_DISTANCE_M = 300.0
MAX_PAIR_DISTANCE_M = 800.0
SAFE_ROUTE_EXCLUSION_M = 300.0
MAX_ACCEPTABLE_ROUTE_DISTANCE_M = 1600.0


def read_tmap_api_key():
    with open(APPLICATION_PROPERTIES, encoding="utf-8") as f:
        for line in f:
            match = re.match(r"^TMAP_API_KEY=(.+)$", line.strip())
            if match:
                return match.group(1).strip()
    raise RuntimeError("TMAP_API_KEY를 application.properties에서 찾지 못했습니다.")


def haversine_m(lat1, lon1, lat2, lon2):
    earth_radius = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlambda / 2) ** 2
    return earth_radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def build_anchor_pool(facilities):
    pool = []
    for facility_type in ("CCTV", "STREET_LIGHT"):
        df = facilities[facility_type]
        for lat, lon in zip(df["latitude"], df["longitude"]):
            pool.append((lat, lon))
    return pool


def is_far_from_safe_routes(lat, lon, safe_route_points, min_distance_m):
    for r_lat, r_lon in safe_route_points:
        if haversine_m(lat, lon, r_lat, r_lon) < min_distance_m:
            return False
    return True


def flatten_safe_route_points(routes):
    points = []
    for r in routes:
        points.extend(r["points"])
    return points


def pick_pairs(anchor_pool, safe_route_points, count, rng, max_attempts=5000):
    pairs = []
    attempts = 0
    while len(pairs) < count and attempts < max_attempts:
        attempts += 1
        a = rng.choice(anchor_pool)
        if not is_far_from_safe_routes(a[0], a[1], safe_route_points, SAFE_ROUTE_EXCLUSION_M):
            continue

        candidates = rng.sample(anchor_pool, min(300, len(anchor_pool)))
        b = None
        for c in candidates:
            d = haversine_m(a[0], a[1], c[0], c[1])
            if MIN_PAIR_DISTANCE_M <= d <= MAX_PAIR_DISTANCE_M:
                b = c
                break
        if b is None:
            continue
        if not is_far_from_safe_routes(b[0], b[1], safe_route_points, SAFE_ROUTE_EXCLUSION_M):
            continue

        pairs.append((a, b))
    return pairs


def request_tmap_route(api_key, start, end):
    headers = {
        "appKey": api_key,
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    body = {
        "startX": start[1],
        "startY": start[0],
        "endX": end[1],
        "endY": end[0],
        "startName": urllib.parse.quote("출발지"),
        "endName": urllib.parse.quote("도착지"),
        "reqCoordType": "WGS84GEO",
        "resCoordType": "WGS84GEO",
        "searchOption": "4",
        "sort": "index",
    }
    response = requests.post(PEDESTRIAN_ROUTE_URL, headers=headers, json=body, timeout=12)
    response.raise_for_status()
    return response.json()


def parse_tmap_response(data):
    points = []
    distance_m = None
    for feature in data.get("features", []):
        properties = feature.get("properties", {})
        if distance_m is None and properties.get("totalDistance"):
            distance_m = properties["totalDistance"]
        geometry = feature.get("geometry", {})
        geom_type = geometry.get("type")
        coordinates = geometry.get("coordinates", [])
        if geom_type == "LineString":
            for lon, lat in coordinates:
                points.append((lat, lon))
        elif geom_type == "MultiLineString":
            for line in coordinates:
                for lon, lat in line:
                    points.append((lat, lon))
    if len(points) < 2:
        return None
    return {"points": points, "distance_m": distance_m}


def load_cache():
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            return json.load(f)
    return []


def save_cache(cache):
    os.makedirs(os.path.dirname(CACHE_PATH), exist_ok=True)
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2)


def get_or_generate(facilities, safe_routes, target_count=50, seed=42):
    cache = load_cache()
    if len(cache) >= target_count:
        print(f"캐시에서 {len(cache)}건 로드 (TMAP 호출 없음)")
        return cache[:target_count]

    api_key = read_tmap_api_key()
    rng = random.Random(seed)
    anchor_pool = build_anchor_pool(facilities)
    safe_route_points = flatten_safe_route_points(safe_routes)

    needed = target_count - len(cache)
    pairs = pick_pairs(anchor_pool, safe_route_points, needed, rng)
    print(f"후보 좌표쌍 {len(pairs)}개 생성, TMAP 호출 시작 (기존 캐시 {len(cache)}건)")

    for i, (start, end) in enumerate(pairs):
        try:
            data = request_tmap_route(api_key, start, end)
            parsed = parse_tmap_response(data)
            if parsed is None:
                print(f"  [{i+1}/{len(pairs)}] 경로 없음, 스킵")
                continue
            if parsed["distance_m"] and parsed["distance_m"] > MAX_ACCEPTABLE_ROUTE_DISTANCE_M:
                print(f"  [{i+1}/{len(pairs)}] 거리 {parsed['distance_m']}m 초과, 스킵")
                continue
            cache.append({
                "route_id": f"CMP-{len(cache)+1:03d}",
                "start": start,
                "end": end,
                "points": parsed["points"],
                "distance_m": parsed["distance_m"],
            })
            print(f"  [{i+1}/{len(pairs)}] 성공, 거리={parsed['distance_m']}m")
        except requests.exceptions.RequestException as e:
            print(f"  [{i+1}/{len(pairs)}] TMAP 호출 실패, 스킵: {e}")
        time.sleep(0.15)

    save_cache(cache)
    print(f"총 {len(cache)}건 저장 완료 -> {CACHE_PATH}")
    return cache[:target_count]


if __name__ == "__main__":
    from facilities import load_facilities
    from load_safe_routes import load_routes

    facilities = load_facilities("../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv")
    safe_routes = load_routes("../backend/src/main/resources/safety_data/안심귀갓길 경로 데이터_수정.shp")

    result = get_or_generate(facilities, safe_routes, target_count=3)
    for r in result:
        print(r["route_id"], r["distance_m"], len(r["points"]))