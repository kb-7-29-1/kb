package com.salgosipo.safety.controller;

import com.salgosipo.safety.dto.SafetyBatchRequestDTO;
import com.salgosipo.safety.dto.SafetyBatchResponseDTO;
import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.SafetyRouteResponseDTO;
import com.salgosipo.safety.service.SafetyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    /**
     * 동일한 propertyId + destinationId의 안전점수와 경로가 모두 DB에 있으면 캐시를 반환합니다.
     * 둘 중 하나라도 없으면 TMAP 대로 우선 경로 한 건을 계산해 점수와 경로를 함께 저장합니다.
     */
    @PostMapping({"/score", "/routes/recommend"})
    public ResponseEntity<SafetyRouteResponseDTO> getOrCalculateSafety(
            @RequestBody SafetyRouteRequestDTO request
    ) {
        return ResponseEntity.ok(safetyService.getOrCalculateSafety(request));
    }

    @PostMapping("/score/details")
    public ResponseEntity<SafetyRouteResponseDTO> calculateSafetyDetails(
            @RequestBody SafetyRouteRequestDTO request
    ) {
        return ResponseEntity.ok(safetyService.calculateSafetyDetails(request));
    }

    /**
     * 메인 화면용 배치 API입니다.
     * 요청한 모든 매물-목적지 조합을 DB에서 먼저 조회하고,
     * 없는 조합만 TMAP을 한 번씩 호출해 계산합니다.
     */
    @PostMapping("/scores/batch")
    public ResponseEntity<SafetyBatchResponseDTO> getOrCalculateSafetyBatch(
            @RequestBody SafetyBatchRequestDTO request
    ) {
        return ResponseEntity.ok(
                safetyService.getOrCalculateSafetyBatch(request)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleCalculationFailure(
            IllegalStateException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }
}
