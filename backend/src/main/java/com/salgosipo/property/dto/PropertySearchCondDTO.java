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
    private Integer minDeposit; // 최소 보증금 (만원)
    private Integer maxDeposit; // 최대 보증금 (만원)
    private Integer minMonthlyRent; // 최소 월세 (만원)
    private Integer maxMonthlyRent; // 최대 월세 (만원)

    // 1) 중심점 기반 검색 (위도 1개, 경도 1개, 반경 km)
    private Double lat; // 중심 위도
    private Double lng; // 중심 경도
    private Double radius; // 검색 반경 (km)

    // 2) 지도 사각형 영역 기반 검색 (Bounds: 남서쪽 & 북동쪽)
    // 나중에 필요하면 주석 해제 + DB 연동까지 고려
    // private Double swLat; // 남서쪽 위도
    // private Double swLng; // 남서쪽 경도
    // private Double neLat; // 북동쪽 위도
    // private Double neLng; // 북동쪽 경도

    // 정렬 (RECOMMENDED, PRICE_ASC, PRICE_DESC, SAFETY_SCORE_DESC 등)
    private String sort;
}
