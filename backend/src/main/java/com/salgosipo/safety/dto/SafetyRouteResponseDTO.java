package com.salgosipo.safety.dto;

import java.util.List;

public class SafetyRouteResponseDTO {
    private Long propertyId;
    private Integer destinationId;
    private Boolean cacheHit;
    private Boolean persisted;
    private String message;
    private Integer safetyScore;
    private String safetyGrade;
    private Integer cctvCount;
    private Integer streetLampCount;
    private Boolean hasPoliceStation;
    private SafetyRouteCandidateDTO selectedRoute;
    private List<SafetyRouteCandidateDTO> candidateRoutes;

    public SafetyRouteResponseDTO() {
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

    public SafetyRouteCandidateDTO getSelectedRoute() {
        return selectedRoute;
    }

    public void setSelectedRoute(SafetyRouteCandidateDTO selectedRoute) {
        this.selectedRoute = selectedRoute;
    }

    public List<SafetyRouteCandidateDTO> getCandidateRoutes() {
        return candidateRoutes;
    }

    public void setCandidateRoutes(List<SafetyRouteCandidateDTO> candidateRoutes) {
        this.candidateRoutes = candidateRoutes;
    }
}
