package com.salgosipo.safety.dto;

public class RoutePointDTO {
    private Double latitude;
    private Double longitude;

    public RoutePointDTO() {
    }

    public RoutePointDTO(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
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
}
