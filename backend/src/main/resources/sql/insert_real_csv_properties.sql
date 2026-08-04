USE salgosipo_db;

-- 1. 기존 테이블 데이터 초기화
DELETE FROM property_safety;
DELETE FROM property_images;
DELETE FROM properties;

-- 2. 대표 목적지 (세종대학교 & 두나무앤파트너스)
INSERT IGNORE INTO destinations (destination_id, dest_latitude, dest_longitude, dest_name, dest_address)
VALUES 
(1, 37.55020000, 127.07310000, '세종대학교', '서울특별시 광진구 능동로 209'),
(2, 37.50080000, 127.03680000, '두나무앤파트너스', '서울특별시 강남구 테헤란로 125');

-- 3. 마포구, 광진구, 강남구(두나무앤파트너스 인근) 매물 데이터 INSERT
INSERT INTO properties (
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
) VALUES 
-- 마포구 매물
(1, 37.55700000, 126.92500000, '서울특별시 마포구 서교동 45-2 (서교 오피스텔)', 2, 2, 3500, 40, 33.10, 3, '2015', FALSE, 'N'),
(2, 37.54980000, 126.91380000, '서울특별시 마포구 합정동 77-9 (합정 안심 원룸)', 1, 1, 1500, 50, 16.50, 2, '2010', FALSE, 'N'),
(3, 37.56350000, 126.92450000, '서울특별시 마포구 연남동 201-5 (연남 오피스)', 3, 1, 5000, 60, 27.30, 5, '2020', FALSE, 'N'),

-- 두나무앤파트너스 (강남구 역삼동) 도보 3분 인근 매물
(4, 37.50120000, 127.03750000, '서울특별시 강남구 역삼동 737 (한신인터밸리24 오피스텔)', 3, 1, 1000, 95, 28.50, 8, '2018', FALSE, 'N'),
(5, 37.50250000, 127.03550000, '서울특별시 강남구 역삼동 649-10 (역삼 센트럴 푸르지오 City)', 3, 2, 2000, 110, 32.00, 12, '2021', FALSE, 'N'),

-- 세종대학교 (광진구 화양동) 도보 5분 인근 매물
(6, 37.54580000, 127.07050000, '서울특별시 광진구 화양동 21-22 (케이타워 오피스텔 B)', 3, 1, 1000, 80, 18.85, 6, '2013', FALSE, 'N'),
(7, 37.54890000, 127.07120000, '서울특별시 광진구 화양동 111-35 (정익제이타워2차)', 3, 1, 1000, 95, 17.30, 5, '2021', FALSE, 'N')
ON DUPLICATE KEY UPDATE address=VALUES(address), deposit=VALUES(deposit), monthly_rent=VALUES(monthly_rent);

-- 4. 안심점수 데이터 (두나무 및 세종대 목적지 연동)
INSERT INTO property_safety (property_id, destination_id, safety_score, cctv_count, street_lamp_count, has_police_station)
VALUES 
(1, 1, 85, 15, 35, TRUE),
(2, 1, 88, 18, 40, TRUE),
(3, 1, 90, 20, 45, TRUE),
(4, 2, 95, 28, 55, TRUE),
(5, 2, 92, 24, 50, TRUE),
(6, 1, 85, 15, 35, TRUE),
(7, 1, 82, 12, 30, FALSE)
ON DUPLICATE KEY UPDATE safety_score=VALUES(safety_score);