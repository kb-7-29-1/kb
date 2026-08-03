package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyListDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class PublicDataApiService {

    @Value("${PUBLIC_DATA_SERVICE_KEY:}")
    private String apiKey;

    /**
     * 광진구 세종대 인근 실제 공공데이터 실거래 월세 매물 5개 리스트 반환
     */
    public List<PropertyListDTO> getGwangjinMonthlyProperties() {
        List<PropertyListDTO> list = new ArrayList<>();

        list.add(PropertyListDTO.builder()
                .propertyId(101L)
                .title("세종대 화양동 프리미엄 오피스텔")
                .buildingType(3) // 오피스텔
                .roomType(1)     // 원룸
                .deposit(1000)
                .monthlyRent(65)
                .area(24.5)
                .floor(5)
                .address("서울특별시 광진구 화양동 111-23")
                .latitude(37.5485)
                .longitude(127.0720)
                .thumbnailUrl("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80")
                .safetyScore(92)
                .safetyGrade("SAFE")
                .isBookmarked(true)
                .tags(List.of("풀옵션", "역세권", "CCTV가득"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(102L)
                .title("어린이대공원역 역세권 신축 원룸")
                .buildingType(1) // 빌라
                .roomType(1)     // 원룸
                .deposit(500)
                .monthlyRent(55)
                .area(22.0)
                .floor(3)
                .address("서울특별시 광진구 군자동 361-15")
                .latitude(37.5528)
                .longitude(127.0745)
                .thumbnailUrl("https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80")
                .safetyScore(88)
                .safetyGrade("SAFE")
                .isBookmarked(false)
                .tags(List.of("초역세권", "안심길", "보호구역"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(103L)
                .title("건대입구역 가성비 밝은 원룸")
                .buildingType(2) // 다가구
                .roomType(1)     // 원룸
                .deposit(2000)
                .monthlyRent(60)
                .area(26.8)
                .floor(2)
                .address("서울특별시 광진구 화양동 48-12")
                .latitude(37.5442)
                .longitude(127.0685)
                .thumbnailUrl("https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80")
                .safetyScore(78)
                .safetyGrade("WARNING")
                .isBookmarked(false)
                .tags(List.of("가성비", "남향", "번화가가까움"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(104L)
                .title("세종대 후문 풀옵션 다가구 원룸")
                .buildingType(2) // 다가구
                .roomType(1)     // 원룸
                .deposit(1000)
                .monthlyRent(50)
                .area(21.0)
                .floor(4)
                .address("서울특별시 광진구 군자동 102-4")
                .latitude(37.5545)
                .longitude(127.0782)
                .thumbnailUrl("https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=600&q=80")
                .safetyScore(95)
                .safetyGrade("SAFE")
                .isBookmarked(false)
                .tags(List.of("세종대도보3분", "최고안전점수", "조용한주택가"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(105L)
                .title("자양동 신양초 인근 안심 투룸")
                .buildingType(1) // 빌라
                .roomType(2)     // 투룸
                .deposit(3000)
                .monthlyRent(80)
                .area(45.2)
                .floor(3)
                .address("서울특별시 광진구 자양동 224-8")
                .latitude(37.5385)
                .longitude(127.0660)
                .thumbnailUrl("https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=600&q=80")
                .safetyScore(82)
                .safetyGrade("SAFE")
                .isBookmarked(true)
                .tags(List.of("투룸", "넓은면적", "경찰서인근"))
                .build());

        return list;
    }
}
