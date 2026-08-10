package com.salgosipo.safety.dto;

import com.salgosipo.safety.domain.SafetyFacilityVO;

import java.util.List;

/**
 * [임시/테스트 전용] 목적지 주변 CCTV/가로등/파출소 원본 좌표 응답입니다.
 */
public class TestFacilityMapResultDTO {
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationName;
    private List<SafetyFacilityVO> facilities;

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public List<SafetyFacilityVO> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<SafetyFacilityVO> facilities) {
        this.facilities = facilities;
    }
}
