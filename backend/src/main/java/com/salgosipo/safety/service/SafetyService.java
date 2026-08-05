package com.salgosipo.safety.service;

import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.SafetyRouteResponseDTO;

public interface SafetyService {

    /**
     * property_id + destination_id 캐시가 있으면 즉시 반환하고,
     * 없으면 TMAP 경로 후보를 평가한 뒤 property_safety에 최초 1회 저장합니다.
     */
    SafetyRouteResponseDTO getOrCalculateSafety(SafetyRouteRequestDTO request);
}
