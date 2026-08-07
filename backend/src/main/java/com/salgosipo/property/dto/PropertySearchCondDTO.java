package com.salgosipo.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchCondDTO {
    private String keyword;
    private String propertyType; // 원룸, 투룸, 오피스텔 등
    private Integer buildingType; // 건물 종류 (1: 빌라, 2: 다가구, 3: 오피스텔)
    private Integer roomType;     // 방 종류 (1: 원룸, 2: 투룸)
    private Integer minDeposit; // 최소 보증금 (만원)
    private Integer maxDeposit; // 최대 보증금 (만원)
    private Integer minMonthlyRent; // 최소 월세 (만원)
    private Integer maxMonthlyRent; // 최대 월세 (만원)

    // 1) 중심점 기반 검색 (위도 1개, 경도 1개, 반경 km)
    private Double lat; // 중심 위도
    private Double lng; // 중심 경도
    private Double radius; // 검색 반경 (km)

    // 2) 지도 사각형 영역 기반 검색 (Bounds: 남서쪽 & 북동쪽)
    private Double swLat; // 남서쪽 위도
    private Double swLng; // 남서쪽 경도
    private Double neLat; // 북동쪽 위도
    private Double neLng; // 북동쪽 경도

    // 정렬 (RECOMMENDED, PRICE_ASC, PRICE_DESC, SAFETY_SCORE_DESC 등)
    private String sort;

    public String toCacheKey() {
        String latStr = lat != null ? String.format("%.3f", lat) : "";
        String lngStr = lng != null ? String.format("%.3f", lng) : "";
        String radStr = radius != null ? String.format("%.1f", radius) : "";
        return String.format("%s_%s_%s_%s_%s_%s_%s_%s_%s_%s",
                keyword != null ? keyword : "",
                propertyType != null ? propertyType : "",
                minDeposit != null ? minDeposit : 0,
                maxDeposit != null ? maxDeposit : 999999,
                minMonthlyRent != null ? minMonthlyRent : 0,
                maxMonthlyRent != null ? maxMonthlyRent : 9999,
                latStr, lngStr, radStr,
                sort != null ? sort : "");
    }
}
