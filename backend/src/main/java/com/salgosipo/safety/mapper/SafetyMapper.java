package com.salgosipo.safety.mapper;

import com.salgosipo.safety.domain.PropertySafetyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SafetyMapper {

    /**
     * 특정 매물과 목적지 조합의 안전 정보 조회
     */
    PropertySafetyVO selectPropertySafety(
            @Param("propertyId") Long propertyId,
            @Param("destinationId") Long destinationId
    );

    /**
     * 안전 정보 저장
     *
     * 동일한 propertyId, destinationId가 이미 존재하면
     * 기존 값을 수정합니다.
     */
    int upsertPropertySafety(PropertySafetyVO propertySafety);
}