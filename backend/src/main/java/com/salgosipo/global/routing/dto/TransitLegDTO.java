package com.salgosipo.global.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대중교통 세부 이동 구간 (도보, 지하철 노선, 버스 노선 등)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitLegDTO {
    private String mode;                 // "WALK", "SUBWAY", "BUS", "TRAM", "RAIL"
    private String routeName;            // "2호선", "721", "5511"
    private String routeColor;           // "#3CB44A", "#2563EB"
    private Double durationMinutes;      // 구간 소요 시간 (분)
    private Double distanceMeters;       // 구간 이동 거리 (m)
    private String fromName;             // 출발 정류장/역
    private String toName;               // 도착 정류장/역
    private List<TransitPointDTO> routePoints; // 실제 철도 선로 및 버스 도로 GPS 좌표 목록
}
