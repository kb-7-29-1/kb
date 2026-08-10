package com.salgosipo.safety.controller;

import com.salgosipo.safety.dto.TestFacilityMapResultDTO;
import com.salgosipo.safety.dto.TestLineStringPageDTO;
import com.salgosipo.safety.dto.TestLineStringSeedResultDTO;
import com.salgosipo.safety.service.TestLineStringDebugService;
import com.salgosipo.safety.service.TestLineStringSeedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [임시/테스트 전용] 세종대 도보권 매물 linestring 검증용 배치 트리거입니다.
 * 검증이 끝나면 이 컨트롤러는 삭제해도 됩니다.
 *
 * limit 파라미터로 이번 호출에서 실제 TMAP을 몇 번까지 호출할지 제한할 수 있습니다.
 * (예: limit=2로 먼저 시험해보고, 문제없으면 limit 없이 전체 실행)
 * 이미 test_line_string에 저장된 property_id는 몇 번을 다시 호출해도 절대
 * TMAP을 재호출하지 않습니다.
 */
@RestController
@RequestMapping("/api/safety/test")
public class TestLineStringController {

    private final TestLineStringSeedService testLineStringSeedService;
    private final TestLineStringDebugService testLineStringDebugService;

    public TestLineStringController(
            TestLineStringSeedService testLineStringSeedService,
            TestLineStringDebugService testLineStringDebugService
    ) {
        this.testLineStringSeedService = testLineStringSeedService;
        this.testLineStringDebugService = testLineStringDebugService;
    }

    @PostMapping("/seed-linestrings")
    public ResponseEntity<TestLineStringSeedResultDTO> seed(
            @RequestParam Integer destinationId,
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam Double radius,
            @RequestParam(required = false) Integer maxDeposit,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                testLineStringSeedService.seed(destinationId, lat, lng, radius, maxDeposit, userId, limit)
        );
    }

    /**
     * 저장된 경로를 offset/limit으로 페이징 조회하면서 SafetyScoreCalculator로
     * 다시 채점해서 반환합니다. TMAP은 호출하지 않습니다.
     */
    @GetMapping("/linestrings")
    public ResponseEntity<TestLineStringPageDTO> getLineStrings(
            @RequestParam Integer destinationId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(
                testLineStringDebugService.getLineStringPage(destinationId, offset, limit)
        );
    }

    /**
     * 목적지 주변 CCTV/가로등/파출소 원본 좌표를 조회합니다.
     */
    @GetMapping("/facilities")
    public ResponseEntity<TestFacilityMapResultDTO> getFacilities(
            @RequestParam Integer destinationId,
            @RequestParam(required = false) Double marginMeters
    ) {
        return ResponseEntity.ok(
                testLineStringDebugService.getFacilities(destinationId, marginMeters)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}