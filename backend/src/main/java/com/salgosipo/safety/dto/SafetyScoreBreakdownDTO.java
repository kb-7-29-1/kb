package com.salgosipo.safety.dto;

public class SafetyScoreBreakdownDTO {
    private Integer cctvDensityPenalty;
    private Integer cctvCoveragePenalty;
    private Integer streetLightCoveragePenalty;
    private Integer policeStationPenalty;
    private Integer totalPenalty;
    private Integer cctvCount;
    private Integer streetLightCount;
    private Boolean hasPoliceStation;
    private Double cctvAverageGapMeters;
    private Double cctvCoveragePercent;
    private Double streetLightCoveragePercent;

    public SafetyScoreBreakdownDTO() {
    }

    public Integer getCctvDensityPenalty() {
        return cctvDensityPenalty;
    }

    public void setCctvDensityPenalty(Integer cctvDensityPenalty) {
        this.cctvDensityPenalty = cctvDensityPenalty;
    }

    public Integer getCctvCoveragePenalty() {
        return cctvCoveragePenalty;
    }

    public void setCctvCoveragePenalty(Integer cctvCoveragePenalty) {
        this.cctvCoveragePenalty = cctvCoveragePenalty;
    }

    public Integer getStreetLightCoveragePenalty() {
        return streetLightCoveragePenalty;
    }

    public void setStreetLightCoveragePenalty(Integer streetLightCoveragePenalty) {
        this.streetLightCoveragePenalty = streetLightCoveragePenalty;
    }

    public Integer getPoliceStationPenalty() {
        return policeStationPenalty;
    }

    public void setPoliceStationPenalty(Integer policeStationPenalty) {
        this.policeStationPenalty = policeStationPenalty;
    }

    public Integer getTotalPenalty() {
        return totalPenalty;
    }

    public void setTotalPenalty(Integer totalPenalty) {
        this.totalPenalty = totalPenalty;
    }

    public Integer getCctvCount() {
        return cctvCount;
    }

    public void setCctvCount(Integer cctvCount) {
        this.cctvCount = cctvCount;
    }

    public Integer getStreetLightCount() {
        return streetLightCount;
    }

    public void setStreetLightCount(Integer streetLightCount) {
        this.streetLightCount = streetLightCount;
    }

    public Boolean getHasPoliceStation() {
        return hasPoliceStation;
    }

    public void setHasPoliceStation(Boolean hasPoliceStation) {
        this.hasPoliceStation = hasPoliceStation;
    }

    public Double getCctvAverageGapMeters() {
        return cctvAverageGapMeters;
    }

    public void setCctvAverageGapMeters(Double cctvAverageGapMeters) {
        this.cctvAverageGapMeters = cctvAverageGapMeters;
    }

    public Double getCctvCoveragePercent() {
        return cctvCoveragePercent;
    }

    public void setCctvCoveragePercent(Double cctvCoveragePercent) {
        this.cctvCoveragePercent = cctvCoveragePercent;
    }

    public Double getStreetLightCoveragePercent() {
        return streetLightCoveragePercent;
    }

    public void setStreetLightCoveragePercent(Double streetLightCoveragePercent) {
        this.streetLightCoveragePercent = streetLightCoveragePercent;
    }
}
