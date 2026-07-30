CREATE DATABASE IF NOT EXISTS salgosipo_db;
CREATE USER IF NOT EXISTS 'salgosipo'@'localhost' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON salgosipo_db.* TO 'salgosipo'@'localhost';
FLUSH PRIVILEGES;

USE salgosipo_db;

-- 외래키 참조하는 자식 테이블부터 삭제
DROP TABLE IF EXISTS route_vote;
DROP TABLE IF EXISTS property_safety;
DROP TABLE IF EXISTS property_amenities;
DROP TABLE IF EXISTS property_tag;
DROP TABLE IF EXISTS property_comment;
DROP TABLE IF EXISTS property_images;
DROP TABLE IF EXISTS bookmarks;
DROP TABLE IF EXISTS onboardings;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS destinations;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    login_id   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    birth_date DATE         NOT NULL,
    gender     CHAR(1)      NOT NULL,
    email      VARCHAR(100) NOT NULL,
    del_yn     CHAR(1)      NOT NULL DEFAULT 'N',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_gender CHECK (gender IN ('M', 'F')),
    CONSTRAINT chk_users_del_yn CHECK (del_yn IN ('N', 'Y'))
);

CREATE TABLE destinations (
    destination_id INT AUTO_INCREMENT PRIMARY KEY,
    dest_latitude  DECIMAL(12, 8) NOT NULL,
    dest_longitude DECIMAL(12, 8) NOT NULL,
    dest_name      VARCHAR(100)   NOT NULL,
    dest_address   VARCHAR(255)   NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_dest_latitude CHECK (dest_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_dest_longitude CHECK (dest_longitude BETWEEN -180 AND 180),
    UNIQUE KEY uq_dest_location (dest_latitude, dest_longitude),
    INDEX idx_destinations_location (dest_latitude, dest_longitude)
);

CREATE TABLE properties (
    property_id         INT AUTO_INCREMENT PRIMARY KEY,
    property_latitude   DECIMAL(12, 8) NOT NULL,
    property_longitude  DECIMAL(12, 8) NOT NULL,
    address             VARCHAR(255)   NOT NULL,
    building_type       TINYINT        NOT NULL,
    room_type           TINYINT        NOT NULL,
    deposit             INT            NOT NULL DEFAULT 0,
    monthly_rent        INT            NOT NULL DEFAULT 0,
    area                DECIMAL(6, 2)  NOT NULL,
    floor               TINYINT        NOT NULL,
    built_year          CHAR(4)        NOT NULL,
    is_illegal_building BOOLEAN        NOT NULL DEFAULT FALSE,
    del_yn              CHAR(1)        NOT NULL DEFAULT 'N',
    CONSTRAINT chk_property_latitude CHECK (property_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_property_longitude CHECK (property_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_properties_del_yn CHECK (del_yn IN ('N', 'Y')),
    UNIQUE KEY uq_property_location (property_latitude, property_longitude),
    INDEX idx_properties_location (property_latitude, property_longitude)
);

CREATE TABLE onboardings (
    user_id          INT NOT NULL PRIMARY KEY,
    destination_id   INT NOT NULL,
    destination_type ENUM('SCHOOL', 'WORK', 'ETC') NOT NULL DEFAULT 'WORK',
    transport_mode   ENUM('WALK', 'TRANSIT') NOT NULL DEFAULT 'TRANSIT',
    max_travel_time  SMALLINT NOT NULL DEFAULT 15,
    budget_deposit   INT NOT NULL,
    budget_rent      INT NOT NULL,
    min_safety_score TINYINT NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_onboardings_users
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_onboardings_destinations
        FOREIGN KEY (destination_id) REFERENCES destinations(destination_id),
    CONSTRAINT chk_onboarding_max_travel_time CHECK (max_travel_time >= 0),
    CONSTRAINT chk_onboarding_budget_deposit CHECK (budget_deposit >= 0),
    CONSTRAINT chk_onboarding_budget_rent CHECK (budget_rent >= 0),
    CONSTRAINT chk_onboarding_safety_score CHECK (min_safety_score BETWEEN 0 AND 100)
);

CREATE TABLE property_images (
    image_id      INT AUTO_INCREMENT PRIMARY KEY,
    property_id   INT          NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    display_order TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pi_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    INDEX idx_pi_property_id (property_id)
);

CREATE TABLE property_comment (
    comment_id   INT AUTO_INCREMENT PRIMARY KEY,
    property_id  INT          NOT NULL,
    user_id      INT          NOT NULL,
    content      VARCHAR(255) NOT NULL,
    del_yn       CHAR(1)      NOT NULL DEFAULT 'N',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    CONSTRAINT fk_comment_users
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_comment_del_yn CHECK (del_yn IN ('N', 'Y')),
    INDEX idx_comment_property (property_id, del_yn)
);

CREATE TABLE property_tag (
    property_id INT      NOT NULL,
    tag_type    TINYINT  NOT NULL,
    tag_count   INT      NOT NULL DEFAULT 1,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (property_id, tag_type),
    CONSTRAINT fk_tag_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    CONSTRAINT chk_property_tag_count CHECK (tag_count >= 0)
);

CREATE TABLE property_safety (
    property_id        INT               NOT NULL,
    destination_id     INT               NOT NULL,
    safety_score       TINYINT UNSIGNED  NOT NULL DEFAULT 0,
    cctv_count         SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    street_lamp_count  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    has_police_station BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (property_id, destination_id),
    CONSTRAINT fk_safety_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    CONSTRAINT fk_safety_destinations
        FOREIGN KEY (destination_id) REFERENCES destinations(destination_id),
    CONSTRAINT chk_property_safety_score CHECK (safety_score BETWEEN 0 AND 100),
    INDEX idx_safety_destination (destination_id)
);

CREATE TABLE route_vote (
    vote_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    property_id    INT NOT NULL,
    destination_id INT NOT NULL,
    vote_type      ENUM('SAFE', 'UNSAFE') NOT NULL,
    del_yn         CHAR(1) NOT NULL DEFAULT 'N',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vote_users
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_vote_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    CONSTRAINT fk_vote_destinations
        FOREIGN KEY (destination_id) REFERENCES destinations(destination_id),
    CONSTRAINT chk_vote_del_yn CHECK (del_yn IN ('N', 'Y')),
    UNIQUE KEY uq_user_route (user_id, property_id, destination_id)
);

CREATE TABLE property_amenities (
    property_id       INT               NOT NULL,
    amenity_type      TINYINT           NOT NULL,
    amenity_name      VARCHAR(100)      NOT NULL,
    amenity_latitude  DECIMAL(12, 8)    NOT NULL,
    amenity_longitude DECIMAL(12, 8)    NOT NULL,
    distance_meters   SMALLINT UNSIGNED NOT NULL,
    walk_time_minutes TINYINT UNSIGNED  NOT NULL,
    created_at        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (property_id, amenity_type),
    CONSTRAINT fk_amenities_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id),
    CONSTRAINT chk_amenity_latitude CHECK (amenity_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_amenity_longitude CHECK (amenity_longitude BETWEEN -180 AND 180)
);

CREATE TABLE bookmarks (
    user_id     INT      NOT NULL,
    property_id INT      NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, property_id),
    CONSTRAINT fk_bookmarks_users
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_bookmarks_properties
        FOREIGN KEY (property_id) REFERENCES properties(property_id)
);
