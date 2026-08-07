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
}
