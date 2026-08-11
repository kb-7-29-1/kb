import shapefile


def load_routes(shp_path):
    sf = shapefile.Reader(shp_path, encoding="euc-kr")
    routes = []
    for shape_record in sf.iterShapeRecords():
        shape = shape_record.shape
        record = shape_record.record.as_dict()
        points = [(lat, lon) for lon, lat in shape.points]
        if len(points) < 2:
            continue
        routes.append({
            "route_id": record.get("안심귀갓_3") or record.get("안심귀갓_4"),
            "gu": record.get("시군구명"),
            "dong": record.get("읍면동명"),
            "built_year": record.get("조성년월"),
            "points": points,
        })
    return routes


if __name__ == "__main__":
    routes = load_routes("../backend/src/main/resources/safety_data/안심귀갓길 경로 데이터_수정.shp")
    print("routes:", len(routes))
    print(routes[0])
