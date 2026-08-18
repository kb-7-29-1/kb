package com.salgosipo.global.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitSummaryDTO {
    // [대중교통 풀코스: 발할라(도보) + 호퍼(탑승) + 발할라(도보)]
    private Double totalTimeMinutes;        // 총 소요 시간 (도보 + 대중교통 탑승)
    private Double totalDistanceKm;         // 총 이동 거리 (km)
    
    private Double accessWalkMinutes;       // 1) 출발지 -> 탑승역 발할라 도보 시간 (분)
    private Double hopperTransitMinutes;    // 2) 역 -> 도착역 호퍼 지하철/버스 순수 탑승 시간 (분)
    private Double egressWalkMinutes;       // 3) 하차역 -> 최종목적지 발할라 도보 시간 (분)

    private Double totalWalkTimeMinutes;    // 총 도보 시간 (1번 + 3번 발할라 도보 합산)
    private Integer transferCount;          // 환승 횟수
    private String routeSummary;            // 노선 요약 (예: 도보 -> 2호선 -> 도보)

    // GTFS 실제 정류장/역 및 노선 세부 정보
    private String transitType;             // "BUS" | "SUBWAY"
    private String routeShortName;          // 버스 번호(예: "721") 또는 지하철 호선(예: "7")
    private String routeColor;              // 노선 고유 Hex Color (예: "#00a84d")
    private String departureLocation;       // 탑승 정류장/역 이름 (예: "어린이대공원역")
    private String arrivalLocation;         // 하차 정류장/역 이름 (예: "건대입구역")

    // 실제 지하철 선로 및 버스 도로 세부 구간 목록 (GPS 좌표 포함)
    private java.util.List<TransitLegDTO> legs;
}
