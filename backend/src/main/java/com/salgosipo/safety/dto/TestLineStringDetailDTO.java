package com.salgosipo.safety.dto;

import java.util.List;

/**
 * [임시/테스트 전용] 저장된 test_line_string 1건을 지도 시각화용으로
 * 다시 채점(SafetyScoreCalculator)한 결과입니다. TMAP은 호출하지 않습니다.
 */
public class TestLineStringDetailDTO {
    private Long propertyId;
    private Integer destinationId;
    private List<RoutePointDTO> routePoints;
    private Integer safetyScore;
    private String safetyGrade;
    private SafetyScoreBreakdownDTO breakdown;

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

    public List<RoutePointDTO> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<RoutePointDTO> routePoints) {
        this.routePoints = routePoints;
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

    public SafetyScoreBreakdownDTO getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(SafetyScoreBreakdownDTO breakdown) {
        this.breakdown = breakdown;
    }
}
