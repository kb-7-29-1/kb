import csv
import os

import pandas as pd

BOAN_DIR = "../backend/src/main/resources/public_data/boan"
NORMALIZED_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized.csv"
OUTPUT_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv"

SEOUL_LAT_MIN, SEOUL_LAT_MAX = 37.3, 37.8
SEOUL_LON_MIN, SEOUL_LON_MAX = 126.6, 127.3

SOURCES = [
    dict(file="서울특별시 강동구_보안등위치현황_20240516.csv", encoding="utf-8-sig",
         lat_idx=1, lon_idx=2, name_idx=0, gu="강동구"),
    dict(file="서울특별시 관악구_스마트폴 정보_20251127.csv", encoding="euc-kr",
         lat_idx=3, lon_idx=4, name_idx=2, gu="관악구",
         filter_fn=lambda row: len(row) > 5 and row[5].strip() == "Y"),
    dict(file="서울특별시 동작구_보안등 현황 및 위치 정보_20260406.csv", encoding="euc-kr",
         lat_idx=3, lon_idx=4, name_idx=0, gu="동작구"),
    dict(file="서울특별시 서대문구_보안등 위치정보_20250101.csv", encoding="euc-kr",
         lat_idx=3, lon_idx=2, name_idx=0, gu="서대문구"),
    dict(file="서울특별시 서초구_보안등 현황_20240513.csv", encoding="euc-kr",
         lat_idx=4, lon_idx=5, name_idx=1, gu="서초구"),
    dict(file="서울특별시 종로구_보안등.csv", encoding="euc-kr",
         lat_idx=4, lon_idx=5, name_idx=0, gu="종로구"),
    dict(file="서울특별시+도봉구_보안등정보_20170517_1495166022473_962364.csv", encoding="euc-kr",
         lat_idx=4, lon_idx=5, name_idx=0, gu="도봉구"),
    dict(file="서울특별시_은평구_보안등정보_20260226.csv", encoding="euc-kr",
         lat_idx=4, lon_idx=5, name_idx=0, gu="은평구"),
    dict(file="NOWON_SMART_LIGHT_2026.05.04-05.10.csv", encoding="euc-kr",
         lat_idx=3, lon_idx=4, name_idx=2, gu="노원구", dedup_idx=2),
    dict(file="YC_SMART_LIGHT_2025.05.12-05.18.csv", encoding="euc-kr",
         lat_idx=3, lon_idx=4, name_idx=2, gu="양천구", dedup_idx=2),
]


def fix_lat_lon(a, b):
    a_is_lat = SEOUL_LAT_MIN <= a <= SEOUL_LAT_MAX
    a_is_lon = SEOUL_LON_MIN <= a <= SEOUL_LON_MAX
    b_is_lat = SEOUL_LAT_MIN <= b <= SEOUL_LAT_MAX
    b_is_lon = SEOUL_LON_MIN <= b <= SEOUL_LON_MAX
    if a_is_lat and b_is_lon:
        return a, b
    if b_is_lat and a_is_lon:
        return b, a
    return None


def load_source(spec):
    path = os.path.join(BOAN_DIR, spec["file"])
    rows = []
    seen = set()
    with open(path, encoding=spec["encoding"], errors="replace", newline="") as f:
        reader = csv.reader(f)
        next(reader, None)
        for row in reader:
            if len(row) <= max(spec["lat_idx"], spec["lon_idx"]):
                continue
            filter_fn = spec.get("filter_fn")
            if filter_fn and not filter_fn(row):
                continue
            dedup_idx = spec.get("dedup_idx")
            if dedup_idx is not None:
                key = row[dedup_idx]
                if key in seen:
                    continue
                seen.add(key)
            try:
                a = float(row[spec["lat_idx"]].strip())
                b = float(row[spec["lon_idx"]].strip())
            except (ValueError, IndexError):
                continue
            fixed = fix_lat_lon(a, b)
            if fixed is None:
                continue
            lat, lon = fixed
            name_idx = spec.get("name_idx")
            name = row[name_idx].strip() if name_idx is not None and name_idx < len(row) else ""
            rows.append({
                "facility_type": "STREET_LIGHT",
                "facility_name": f"{spec['gu']} {name}".strip(),
                "latitude": lat,
                "longitude": lon,
                "facility_count": 1,
                "source_name": spec["file"],
                "source_key": f"{spec['gu']}_{len(rows) + 1}",
            })
    return rows


def main():
    all_rows = []
    print(f"{'구':10s} {'파일':55s} {'건수':>8s}")
    for spec in SOURCES:
        rows = load_source(spec)
        all_rows.extend(rows)
        print(f"{spec['gu']:10s} {spec['file']:55s} {len(rows):>8d}")

    print("총 추가 건수:", len(all_rows))

    new_df = pd.DataFrame(all_rows)
    existing_df = pd.read_csv(NORMALIZED_CSV)

    merged_df = pd.concat([existing_df, new_df], ignore_index=True)
    merged_df.to_csv(OUTPUT_CSV, index=False)

    print("원본 파일은 그대로 둠:", NORMALIZED_CSV)
    print("새 파일 저장:", OUTPUT_CSV)
    print("기존 STREET_LIGHT:", (existing_df.facility_type == "STREET_LIGHT").sum())
    print("병합 후 STREET_LIGHT:", (merged_df.facility_type == "STREET_LIGHT").sum())
    print("병합 후 전체 행:", len(merged_df))


if __name__ == "__main__":
    main()
