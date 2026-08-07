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
    private String builtYear;      // 준공연도 (YYYY)
    private Boolean isIllegalBuilding; // 위반 건축물 여부
    private String illegalReason;  // 위반/적법 지정 사유
    private String useAprDay;      // 사용승인일 (YYYYMMDD)
    private String structureName;  // 건축물 구조
    private String mainPurposeName;// 주용도
    private String earthquakeProofYn; // 내진설계 여부 (1/0)
    private String address;
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    private String thumbnailUrl;   // 대표 이미지 URL
    private Integer safetyScore;   // 100점 만점 안전점수
    private String safetyGrade;    // SAFE, WARNING, DANGER
    private Boolean isBookmarked;  // 로그인 유저의 찜 여부
    private String dataSource;     // DB 또는 PUBLIC_API
    private Integer dealCount;     // 동일 건물/위치 실거래 건수
    private List<String> tags;     // 주요 태그 목록
}
