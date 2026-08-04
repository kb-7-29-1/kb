USE salgosipo_db;

-- 기존 테이블 초기화
DELETE FROM property_safety WHERE property_id BETWEEN 101 AND 110;

DELETE FROM property_images WHERE property_id BETWEEN 101 AND 110;

DELETE FROM properties WHERE property_id BETWEEN 101 AND 110;

-- 1. 대표 목적지 (세종대학교)
INSERT IGNORE INTO
    destinations (
        destination_id,
        dest_latitude,
        dest_longitude,
        dest_name,
        dest_address
    )
VALUES (
        1,
        37.55020000,
        127.07310000,
        '세종대학교',
        '서울특별시 광진구 능동로 209'
    );

-- 2. public_data/seoul_officetel_raw.csv 광진구 실제 거래 매물 데이터 (건물명, 주소, 보증금, 월세, 전용면적, 층, 건축년도 100% 매칭)
INSERT INTO
    properties (
        property_id,
        property_latitude,
        property_longitude,
        address,
        building_type,
        room_type,
        deposit,
        monthly_rent,
        area,
        floor,
        built_year,
        is_illegal_building,
        del_yn
    )
VALUES (
        101,
        37.54580000,
        127.07050000,
        '서울특별시 광진구 화양동 21-22 (케이타워 오피스텔 B)',
        3,
        1,
        1000,
        80,
        18.85,
        6,
        '2013',
        FALSE,
        'N'
    ),
    (
        102,
        37.53750000,
        127.06820000,
        '서울특별시 광진구 자양동 779 (타워더모스트광진아크로텔)',
        3,
        1,
        1000,
        79,
        22.76,
        8,
        '2014',
        FALSE,
        'N'
    ),
    (
        103,
        37.54320000,
        127.06980000,
        '서울특별시 광진구 화양동 174 (건대 트레비앙 오피스텔)',
        3,
        1,
        2000,
        100,
        17.46,
        6,
        '2023',
        FALSE,
        'N'
    ),
    (
        104,
        37.54890000,
        127.07120000,
        '서울특별시 광진구 화양동 111-35 (정익제이타워2차)',
        3,
        1,
        1000,
        95,
        17.30,
        5,
        '2021',
        FALSE,
        'N'
    ),
    (
        105,
        37.55820000,
        127.08150000,
        '서울특별시 광진구 중곡동 639-4 (정익제이타워1차)',
        3,
        1,
        1000,
        82,
        16.37,
        13,
        '2018',
        FALSE,
        'N'
    ),
    (
        106,
        37.54280000,
        127.08450000,
        '서울특별시 광진구 구의동 218-12 (다성리버텔)',
        3,
        1,
        1000,
        85,
        33.97,
        9,
        '2002',
        FALSE,
        'N'
    ),
    (
        107,
        37.53680000,
        127.07150000,
        '서울특별시 광진구 자양동 859 (래미안프리미어팰리스)',
        3,
        1,
        25000,
        0,
        35.82,
        4,
        '2017',
        FALSE,
        'N'
    )
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    deposit = VALUES(deposit),
    monthly_rent = VALUES(monthly_rent);

-- 3. 실거래 매물 안심점수 연동
INSERT INTO
    property_safety (
        property_id,
        destination_id,
        safety_score,
        cctv_count,
        street_lamp_count,
        has_police_station
    )
VALUES (101, 1, 85, 15, 35, TRUE),
    (102, 1, 88, 18, 40, TRUE),
    (103, 1, 90, 20, 45, TRUE),
    (104, 1, 82, 12, 30, FALSE),
    (105, 1, 80, 10, 28, FALSE),
    (106, 1, 79, 11, 26, FALSE),
    (107, 1, 93, 25, 50, TRUE)
ON DUPLICATE KEY UPDATE
    safety_score = VALUES(safety_score);