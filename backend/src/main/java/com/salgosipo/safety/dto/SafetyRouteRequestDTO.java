package com.salgosipo.safety.dto;

/**
 * 매물과 목적지 조합의 안전점수를 조회하거나 새로 계산할 때 사용하는 요청 DTO입니다.
 *
 * propertyId는 property_safety 외래키와 캐시 키로 사용되므로 필수입니다.
 * destinationId가 있으면 DB의 목적지 정보를 사용합니다.
 * destinationId가 없으면 목적지 좌표와 이름을 이용해 destinations에 먼저 저장합니다.
 * 매물 좌표는 조작 방지를 위해 properties 테이블 값을 우선 사용합니다.
 */
public class SafetyRouteRequestDTO {
    private Long propertyId;
    private String propertyName;
    private Double propertyLatitude;
    private Double propertyLongitude;
    private Integer destinationId;
    private String destinationName;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;

    public SafetyRouteRequestDTO() {
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Double getPropertyLatitude() {
        return propertyLatitude;
    }

    public void setPropertyLatitude(Double propertyLatitude) {
        this.propertyLatitude = propertyLatitude;
    }

    public Double getPropertyLongitude() {
        return propertyLongitude;
    }

    public void setPropertyLongitude(Double propertyLongitude) {
        this.propertyLongitude = propertyLongitude;
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
