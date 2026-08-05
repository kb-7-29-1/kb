package com.salgosipo.safety.domain;

public class SafetyFacilityVO {
    private Long facilityId;
    private String facilityType;
    private String facilityName;
    private Double latitude;
    private Double longitude;
    private Integer facilityCount;

    public SafetyFacilityVO() {
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getFacilityCount() {
        return facilityCount;
    }

    public void setFacilityCount(Integer facilityCount) {
        this.facilityCount = facilityCount;
    }
}
