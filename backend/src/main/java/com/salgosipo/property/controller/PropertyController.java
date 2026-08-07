package com.salgosipo.property.controller;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * 매물 목록 조회 (필터링, 5종 정렬, 지도 사각형 Bounds / 중심점 검색)
     * GET /api/properties
     */
    @GetMapping
    public ResponseEntity<List<PropertyListDTO>> getPropertyList(
            @ModelAttribute PropertySearchCondDTO cond,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        List<PropertyListDTO> list = propertyService.getPropertyList(cond, userId);
        return ResponseEntity.ok(list);
    }

    /**
     * 매물 상세 정보 조회 (우측 슬라이딩 도어 패널용)
     * destinationId가 있으면 해당 목적지와의 안전점수만 조인합니다.
     */
    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyDetailDTO> getPropertyDetail(
            @PathVariable("propertyId") Long propertyId,
            @RequestParam(value = "destinationId", required = false)
            Integer destinationId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        PropertyDetailDTO detail = propertyService.getPropertyDetail(
                propertyId,
                destinationId,
                userId
        );
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }
}
