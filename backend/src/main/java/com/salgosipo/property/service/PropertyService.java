package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyDetailDTO;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;

import java.util.List;

public interface PropertyService {
    // 매물 목록 조회 (필터 & 정렬)
    List<PropertyListDTO> getPropertyList(PropertySearchCondDTO cond, Long userId);

    // 매물 상세 정보 조회
    PropertyDetailDTO getPropertyDetail(Long propertyId, Long userId);
}
