package com.salgosipo.safety.dto;

import java.util.List;

/**
 * 메인 화면에 표시할 여러 매물의 안전점수를 한 번에 준비하는 요청 DTO입니다.
 *
 * destinationId가 있으면 저장된 목적지를 사용하고,
 * 없으면 목적지 좌표와 이름으로 destinations 테이블에 먼저 저장합니다.
 */
public class SafetyBatchRequestDTO {
    private List<Long> propertyIds;
    private Integer destinationId;
    private String destinationName;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;

    public SafetyBatchRequestDTO() {
    }

    public List<Long> getPropertyIds() {
        return propertyIds;
    }

    public void setPropertyIds(List<Long> propertyIds) {
        this.propertyIds = propertyIds;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

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
}
