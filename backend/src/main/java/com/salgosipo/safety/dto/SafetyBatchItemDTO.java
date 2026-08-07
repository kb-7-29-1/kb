package com.salgosipo.safety.dto;

/**
 * 배치 안전점수 요청에서 매물 한 건의 처리 결과입니다.
 */
public class SafetyBatchItemDTO {
    private Long propertyId;
    private Integer destinationId;
    private String status;
    private Boolean cacheHit;
    private Boolean persisted;
    private String message;
    private Integer safetyScore;
    private String safetyGrade;
    private Integer cctvCount;
    private Integer streetLampCount;
    private Boolean hasPoliceStation;

    public SafetyBatchItemDTO() {
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public Boolean getPersisted() {
        return persisted;
    }

    public void setPersisted(Boolean persisted) {
        this.persisted = persisted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Integer safetyScore) {
        this.safetyScore = safetyScore;
    }

    public String getSafetyGrade() {
        return safetyGrade;
    }

    public void setSafetyGrade(String safetyGrade) {
        this.safetyGrade = safetyGrade;
    }

    public Integer getCctvCount() {
        return cctvCount;
    }

    public void setCctvCount(Integer cctvCount) {
        this.cctvCount = cctvCount;
    }

    public Integer getStreetLampCount() {
        return streetLampCount;
    }

    public void setStreetLampCount(Integer streetLampCount) {
        this.streetLampCount = streetLampCount;
    }

    public Boolean getHasPoliceStation() {
        return hasPoliceStation;
    }

    public void setHasPoliceStation(Boolean hasPoliceStation) {
        this.hasPoliceStation = hasPoliceStation;
    }
}
