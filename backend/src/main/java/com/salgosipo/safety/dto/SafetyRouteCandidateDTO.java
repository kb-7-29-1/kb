package com.salgosipo.safety.dto;

import java.util.List;

public class SafetyRouteCandidateDTO {
    private String routeId;
    private String searchOption;
    private String routeType;
    private Boolean selected;
    private Integer safetyScore;
    private String safetyGrade;
    private Integer distanceMeters;
    private Integer totalTimeSeconds;
    private SafetyScoreBreakdownDTO breakdown;
    private List<RoutePointDTO> routePoints;

    public SafetyRouteCandidateDTO() {
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

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
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

    public SafetyScoreBreakdownDTO getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(SafetyScoreBreakdownDTO breakdown) {
        this.breakdown = breakdown;
    }

    public List<RoutePointDTO> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<RoutePointDTO> routePoints) {
        this.routePoints = routePoints;
    }
}
