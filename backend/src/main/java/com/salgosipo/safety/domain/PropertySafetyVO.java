package com.salgosipo.safety.domain;

import java.time.LocalDateTime;

public class PropertySafetyVO {
    private Long propertyId;
    private Integer destinationId;
    private Integer safetyScore;
    private Integer cctvCount;
    private Integer streetLampCount;
    private Boolean hasPoliceStation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PropertySafetyVO() {
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

    public Integer getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Integer safetyScore) {
        this.safetyScore = safetyScore;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
