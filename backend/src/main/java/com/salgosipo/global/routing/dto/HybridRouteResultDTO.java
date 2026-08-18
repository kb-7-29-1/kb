package com.salgosipo.global.routing.dto;

import com.salgosipo.safety.domain.PedestrianRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridRouteResultDTO {
    // 1. 발할라 전용 도보 결과
    private Double walkTimeMinutes;      // 발할라 도보 시간 (분)
    private Double walkDistanceKm;       // 발할라 도보 거리 (km)
    private PedestrianRoute walkDetail;  // 발할라 세부 도보 경로 (좌표/점수 연동용)

    // 2. 호퍼 전용 대중교통 결과
    private Double transitTimeMinutes;   // 호퍼 대중교통 총 소요 시간 (분)
    private Double transitDistanceKm;    // 호퍼 대중교통 이동 거리 (km)
    private TransitSummaryDTO transitDetail;

    // 3. 이소크론 시간 필터 판정 (최소/최대 시간 범위 내 포함 여부)
    private Boolean withinTimeRange;     // 지정된 출퇴근 시간 범위(min~max) 만족 여부
}
