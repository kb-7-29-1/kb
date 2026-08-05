package com.salgosipo.safety.repository;

import com.salgosipo.safety.domain.SafetyFacilityVO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 안전시설 원본 좌표를 별도 DB 테이블에 저장하지 않고 classpath CSV에서 읽습니다.
 *
 * 애플리케이션 시작 시 한 번만 CSV를 읽어 0.01도 단위 격자 인덱스를 만들고,
 * 요청 경로 주변의 CCTV·가로등·경찰시설만 빠르게 반환합니다.
 * 외부 CSV 라이브러리를 사용하지 않아 build.gradle 수정이 필요하지 않습니다.
 */
public class SafetyFacilityRepository {

    private static final Logger log = LogManager.getLogger(SafetyFacilityRepository.class);

    private static final double CELL_SIZE_DEGREES = 0.01;
    private static final String DEFAULT_RESOURCE = "public_data/safety_facility_normalized.csv";

    private final Map<GridCell, List<SafetyFacilityVO>> facilitiesByCell;
    private final int facilityCount;

    public SafetyFacilityRepository(String resourceLocation) {
        String normalizedLocation = normalizeResourceLocation(resourceLocation);
        LoadResult loadResult = load(normalizedLocation);
        this.facilitiesByCell = loadResult.facilitiesByCell();
        this.facilityCount = loadResult.facilityCount();
        log.info("안전시설 CSV 로딩 완료: {}건, 격자 {}개", facilityCount, facilitiesByCell.size());
    }

    public List<SafetyFacilityVO> findInBounds(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
        if (facilityCount == 0) {
            return List.of();
        }

        int minLatCell = toCellIndex(minLatitude);
        int maxLatCell = toCellIndex(maxLatitude);
        int minLngCell = toCellIndex(minLongitude);
        int maxLngCell = toCellIndex(maxLongitude);

        List<SafetyFacilityVO> result = new ArrayList<>();
        for (int latCell = minLatCell; latCell <= maxLatCell; latCell++) {
            for (int lngCell = minLngCell; lngCell <= maxLngCell; lngCell++) {
                List<SafetyFacilityVO> cellFacilities = facilitiesByCell.get(
                        new GridCell(latCell, lngCell)
                );
                if (cellFacilities == null) {
                    continue;
                }

                for (SafetyFacilityVO facility : cellFacilities) {
                    double latitude = facility.getLatitude();
                    double longitude = facility.getLongitude();
                    if (latitude >= minLatitude && latitude <= maxLatitude
                            && longitude >= minLongitude && longitude <= maxLongitude) {
                        result.add(facility);
                    }
                }
            }
        }
        return result;
    }

    public int count() {
        return facilityCount;
    }

    private LoadResult load(String resourceLocation) {
        ClassLoader classLoader = SafetyFacilityRepository.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourceLocation);
        if (inputStream == null) {
            throw new IllegalStateException("안전시설 CSV가 없습니다: classpath:" + resourceLocation);
        }

        Map<GridCell, List<SafetyFacilityVO>> mutableIndex = new HashMap<>();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException("안전시설 CSV가 비어 있습니다.");
            }

            List<String> headers = parseCsvLine(removeBom(headerLine));
            Map<String, Integer> headerIndex = createHeaderIndex(headers);
            validateRequiredHeaders(headerIndex);

            String line;
            long syntheticId = 1L;
            long lineNumber = 1L;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                SafetyFacilityVO facility = toFacility(
                        values,
                        headerIndex,
                        syntheticId++,
                        lineNumber
                );
                if (facility == null) {
                    continue;
                }

                GridCell cell = new GridCell(
                        toCellIndex(facility.getLatitude()),
                        toCellIndex(facility.getLongitude())
                );
                mutableIndex.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(facility);
                count++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("안전시설 CSV를 읽지 못했습니다.", e);
        }

        Map<GridCell, List<SafetyFacilityVO>> immutableIndex = new HashMap<>();
        mutableIndex.forEach((cell, facilities) ->
                immutableIndex.put(cell, Collections.unmodifiableList(facilities))
        );
        return new LoadResult(Collections.unmodifiableMap(immutableIndex), count);
    }

    private SafetyFacilityVO toFacility(
            List<String> values,
            Map<String, Integer> headerIndex,
            long syntheticId,
            long lineNumber
    ) {
        try {
            String type = value(values, headerIndex, "facility_type").trim().toUpperCase();
            double latitude = Double.parseDouble(value(values, headerIndex, "latitude").trim());
            double longitude = Double.parseDouble(value(values, headerIndex, "longitude").trim());
            int facilityCount = Math.max(
                    1,
                    Integer.parseInt(value(values, headerIndex, "facility_count").trim())
            );

            if (!("CCTV".equals(type)
                    || "STREET_LIGHT".equals(type)
                    || "POLICE".equals(type))) {
                return null;
            }
            if (latitude < -90.0 || latitude > 90.0
                    || longitude < -180.0 || longitude > 180.0) {
                return null;
            }

            SafetyFacilityVO facility = new SafetyFacilityVO();
            facility.setFacilityId(syntheticId);
            facility.setFacilityType(type);
            facility.setFacilityName(value(values, headerIndex, "facility_name"));
            facility.setLatitude(latitude);
            facility.setLongitude(longitude);
            facility.setFacilityCount(facilityCount);
            return facility;
        } catch (RuntimeException e) {
            log.debug("안전시설 CSV 행 건너뜀: line={}, reason={}", lineNumber, e.getMessage());
            return null;
        }
    }

    private String value(
            List<String> values,
            Map<String, Integer> headerIndex,
            String header
    ) {
        Integer index = headerIndex.get(header);
        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private Map<String, Integer> createHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).trim(), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> required = List.of(
                "facility_type",
                "facility_name",
                "latitude",
                "longitude",
                "facility_count"
        );
        for (String header : required) {
            if (!headerIndex.containsKey(header)) {
                throw new IllegalStateException("안전시설 CSV 필수 컬럼이 없습니다: " + header);
            }
        }
    }

    /**
     * 한 줄 안의 쉼표, 큰따옴표, "" 이스케이프를 처리하는 작은 RFC 4180 파서입니다.
     * 현재 공공데이터는 필드 내부 줄바꿈을 사용하지 않으므로 행 단위로 처리합니다.
     */
    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String normalizeResourceLocation(String resourceLocation) {
        if (resourceLocation == null || resourceLocation.isBlank()) {
            return DEFAULT_RESOURCE;
        }
        String normalized = resourceLocation.trim();
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? DEFAULT_RESOURCE : normalized;
    }

    private String removeBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private int toCellIndex(double coordinate) {
        return (int) Math.floor(coordinate / CELL_SIZE_DEGREES);
    }

    private record GridCell(int latitudeCell, int longitudeCell) {
    }

    private record LoadResult(
            Map<GridCell, List<SafetyFacilityVO>> facilitiesByCell,
            int facilityCount
    ) {
    }
}
