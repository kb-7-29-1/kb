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
public class PropertyDetailDTO {
    private Long propertyId;
    private String title;
    private String description;
    private Integer buildingType;     // 건물 종류 (1: 빌라, 2: 다가구, 3: 오피스텔)
    private Integer roomType;         // 방 종류 (1: 원룸, 2: 투룸)
    private Integer deposit;
    private Integer monthlyRent;
    private Double area;
    private Integer floor;
    private String builtYear;         // 준공연도 (YYYY)
    private String address;
    private Double latitude;
    private Double longitude;
    private List<String> imageUrls;
    private Boolean isBookmarked;     // 로그인 유저의 찜 여부

    // 안전 리포트 정보 (property_safety 테이블 연동)
    private Integer safetyScore;      // 안전점수 (0~100)
    private String safetyGrade;       // 안전등급
    private Integer cctvCount;        // 경로 내 CCTV 수
    private Integer streetlightCount; // 경로 내 가로등/보안등 수
    private Boolean hasPoliceStation; // 경로 내 파출소 존재 여부
    private Boolean isIllegalBuilding;// 위반건축물 여부
    private Integer dealCount;        // 동일 건물/위치 실거래 건수

    private List<String> tags;        // 댓글 키워드 추출 태그
}
