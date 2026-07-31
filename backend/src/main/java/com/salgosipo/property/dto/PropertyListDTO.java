package com.salgosipo.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListDTO {
    private Long propertyId;
    private String title;
    private Integer buildingType;  // 건물 종류 (1: 빌라, 2: 다가구, 3: 오피스텔)
    private Integer roomType;      // 방 종류 (1: 원룸, 2: 투룸)
    private Integer deposit;       // 보증금 (만원)
    private Integer monthlyRent;   // 월세 (만원)
    private Double area;           // 전용면적 (m2)
    private Integer floor;          // 층수
    private String address;
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    private String thumbnailUrl;   // 대표 이미지 URL
    private Integer safetyScore;   // 100점 만점 안전점수
    private String safetyGrade;    // SAFE, WARNING, DANGER
    private Boolean isBookmarked;  // 로그인 유저의 찜 여부
    private List<String> tags;     // 주요 태그 목록
}
