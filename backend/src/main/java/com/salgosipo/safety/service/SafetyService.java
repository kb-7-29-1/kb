package com.salgosipo.safety.service;

import com.salgosipo.safety.dto.SafetyBatchRequestDTO;
import com.salgosipo.safety.dto.SafetyBatchResponseDTO;
import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.SafetyRouteResponseDTO;

public interface SafetyService {

    /**
     * property_id + destination_id의 점수/경로 캐시가 모두 있으면 즉시 반환하고,
     * 없으면 TMAP 대로 우선 경로 한 건을 평가해 점수와 경로를 함께 저장합니다.
     */
    SafetyRouteResponseDTO getOrCalculateSafety(SafetyRouteRequestDTO request);

    SafetyRouteResponseDTO calculateSafetyDetails(SafetyRouteRequestDTO request);

    /**
     * 메인 화면에 표시할 여러 매물의 캐시를 한 번에 조회하고,
     * 캐시가 없는 조합만 TMAP 대로 우선 경로로 계산해 저장합니다.
     */
    SafetyBatchResponseDTO getOrCalculateSafetyBatch(SafetyBatchRequestDTO request);

    /**
     * DB에 이미 저장된 LineString 궤적(cachedRoute)을 TMAP 호출 0건으로 재활용하여
     * SafetyScoreCalculator 정밀 감산 로직으로 초고속 재계산 후 DB에 업데이트합니다.
     */
    com.salgosipo.safety.domain.PropertySafetyVO recalculateFromCachedRoute(
            com.salgosipo.safety.domain.SafetyRouteCacheVO cachedRoute,
            com.salgosipo.safety.domain.SafetyPropertyCoordinateVO property,
            com.salgosipo.safety.domain.SafetyDestinationVO destination
    );

    /**
     * property_id + destination_id에 이미 저장된 경로의 bounding box 안에 있는
     * CCTV/가로등/파출소 원본 좌표를 조회합니다. TMAP은 호출하지 않습니다.
     */
    java.util.List<com.salgosipo.safety.domain.SafetyFacilityVO> getRouteFacilities(
            Long propertyId,
            Integer destinationId
    );
}
