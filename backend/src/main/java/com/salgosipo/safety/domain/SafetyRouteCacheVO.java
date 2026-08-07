package com.salgosipo.safety.domain;

import java.time.LocalDateTime;

/**
 * property_id + destination_id 조합별 TMAP 보행자 경로 캐시입니다.
 * routePointsJson에는 [{"latitude":...,"longitude":...}, ...] 형태의
 * 경로 좌표 배열을 그대로 저장합니다.
 */
public class SafetyRouteCacheVO {
    private Long propertyId;
    private Integer destinationId;
    private String routeId;
    private String searchOption;
    private String routeType;
    private Integer distanceMeters;
    private Integer totalTimeSeconds;
    private String routePointsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SafetyRouteCacheVO() {
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

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Integer totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public String getRoutePointsJson() {
        return routePointsJson;
    }

    public void setRoutePointsJson(String routePointsJson) {
        this.routePointsJson = routePointsJson;
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
