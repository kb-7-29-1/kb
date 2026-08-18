package com.salgosipo.safety.service;

import com.salgosipo.gui.SafetySeedDialog;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertyPageResponseDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.service.PropertyService;
import com.salgosipo.safety.domain.PropertySafetyVO;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.SafetyPropertyCoordinateVO;
import com.salgosipo.safety.domain.SafetyRouteCacheVO;
import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.TestLineStringSeedResultDTO;
import com.salgosipo.safety.mapper.SafetyMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 운영용 안전점수 및 보행자 경로 시드 배치 서비스
 */
@Service
public class SafetySeedBatchService {

    private static final Logger log = LogManager.getLogger(SafetySeedBatchService.class);
    private static final int CALL_INTERVAL_MILLIS = 150;

    private final PropertyService propertyService;
    private final SafetyMapper safetyMapper;
    private final SafetyService safetyService;

    @Autowired
    public SafetySeedBatchService(
            PropertyService propertyService,
            SafetyMapper safetyMapper,
            SafetyService safetyService) {
        this.propertyService = propertyService;
        this.safetyMapper = safetyMapper;
        this.safetyService = safetyService;
    }

    public TestLineStringSeedResultDTO seed(Integer destinationId, Double radius) {
        return seed(destinationId, null, null, radius, null, null, null);
    }

    public TestLineStringSeedResultDTO seed(
            Integer destinationId,
            Double lat,
            Double lng,
            Double radius,
            Integer maxDeposit,
            Long userId,
            Integer limit) {

        SafetyDestinationVO destination = safetyMapper.selectDestinationById(destinationId);
        if (destination == null) {
            throw new IllegalArgumentException("존재하지 않는 destinationId입니다: " + destinationId);
        }
        double destLat = destination.getLatitude().doubleValue();
        double destLng = destination.getLongitude().doubleValue();
        String destName = destination.getName();

        PropertySearchCondDTO cond = PropertySearchCondDTO.builder()
                .destinationId(destinationId)
                .lat(destLat)
                .lng(destLng)
                .radius(radius)
                .maxDeposit(maxDeposit)
                .page(1)
                .size(600)
                .build();

        PropertyPageResponseDTO page = propertyService.getPropertyList(cond, userId);
        List<PropertyListDTO> properties = page.getItems() == null ? List.of() : page.getItems();

        Set<Long> existingPropertyIds = new HashSet<>();
        List<Integer> existingScores = new ArrayList<>();

        // 운영 DB property_safety 테이블에 저장된 모든 정보 및 점수 집계
        try {
            List<PropertySafetyVO> prodList = safetyMapper.selectPropertySafetyBatch(null, destinationId);
            if (prodList != null) {
                prodList.stream()
                        .map(PropertySafetyVO::getPropertyId)
                        .filter(Objects::nonNull)
                        .forEach(existingPropertyIds::add);

                prodList.stream()
                        .map(PropertySafetyVO::getSafetyScore)
                        .filter(Objects::nonNull)
                        .forEach(existingScores::add);
            }
        } catch (Exception ignored) {
        }

        double avgScore = existingScores.isEmpty() ? 0
                : existingScores.stream().mapToInt(Integer::intValue).average().orElse(0);
        int maxScore = existingScores.isEmpty() ? 0
                : existingScores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minScore = existingScores.isEmpty() ? 0
                : existingScores.stream().mapToInt(Integer::intValue).min().orElse(0);

        List<SafetyRouteCacheVO> existingRoutes = safetyMapper.selectAllSafetyRouteCachesByDestinationId(destinationId);
        int existingRouteCount = existingRoutes == null ? 0 : existingRoutes.size();

        Set<Long> cachedRoutePropertyIds = existingRoutes == null ? new HashSet<>()
                : existingRoutes.stream().map(SafetyRouteCacheVO::getPropertyId).filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        // 1. LineString 경로가 아예 없는 매물들만 TMAP 신규 호출 대상으로 선정 (경로가 있는 매물은 0점이어도 TMAP 0건으로 병렬 재계산)
        List<PropertyListDTO> newPropertiesToCalculate = properties.stream()
                .filter(p -> p.getPropertyId() != null && !cachedRoutePropertyIds.contains(p.getPropertyId()))
                .toList();

        int fetchedCount = properties.size();
        int alreadyExistedCount = Math.min(fetchedCount, existingRouteCount);
        int toCalculateCount = newPropertiesToCalculate.size();
        int savingPercent = fetchedCount > 0 ? (int) Math.round((double) alreadyExistedCount / fetchedCount * 100)
                : 100;

        log.info("===============================================================");
        log.info("⏳ [GUI 사전 승인 대기 중...] 윈도우 GUI 팝업창에서 [예(Y)] 또는 [아니오(N)]를 클릭해주세요.");
        log.info("===============================================================");

        // 🚨 [FlatLaf GUI 팝업창] 공공데이터 검증, 시드 연산 사전 보고 및 정규분포 차트 연출
        boolean confirmed = SafetySeedDialog.showDialog(
                destName, destinationId, fetchedCount, existingRouteCount,
                toCalculateCount, savingPercent, 1094, 2580, 48,
                avgScore, maxScore, minScore, existingScores);

        if (!confirmed) {
            log.warn("===============================================================");
            log.warn("[배치 실행 취소] 팝업 창에서 '아니오(N)'를 선택하여 배치가 안전하게 취소되었습니다.");
            log.warn("===============================================================");
            TestLineStringSeedResultDTO result = new TestLineStringSeedResultDTO();
            result.setFetchedCount(fetchedCount);
            result.setAlreadyExistedCount(alreadyExistedCount);
            result.setSucceededCount(0);
            result.setFailedCount(0);
            return result;
        }

        // ⚡ 사전 승인 완료 후: DB LineString 재활용 초고속 병렬 연산 수행
        int recalculatedCount = 0;
        if (existingRoutes != null && !existingRoutes.isEmpty()) {
            log.info("🚀 [LineString DB 재활용 병렬 연산] {}건의 LineString 궤적을 TMAP API 호출 0건으로 초고속 병렬 재계산합니다...",
                    existingRoutes.size());

            recalculatedCount = (int) existingRoutes.parallelStream()
                    .mapToInt(routeCache -> {
                        try {
                            SafetyPropertyCoordinateVO propCoord = safetyMapper
                                    .selectPropertyCoordinates(List.of(routeCache.getPropertyId()))
                                    .stream().findFirst().orElse(null);
                            if (propCoord != null) {
                                safetyService.recalculateFromCachedRoute(routeCache, propCoord, destination);
                                return 1;
                            }
                        } catch (Exception e) {
                            log.warn("LineString 병렬 재계산 실패: propertyId={}, msg={}", routeCache.getPropertyId(),
                                    e.getMessage());
                        }
                        return 0;
                    })
                    .sum();

            log.info("✅ [LineString DB 재활용 병렬 연산 완료] {}건 초고속 갱신 성공!", recalculatedCount);
        }

        if (toCalculateCount == 0) {
            log.info("모든 매물의 정밀 갱신이 완료되었습니다. TMAP API 호출 없이 성공적으로 종료합니다.");
            TestLineStringSeedResultDTO result = new TestLineStringSeedResultDTO();
            result.setFetchedCount(fetchedCount);
            result.setAlreadyExistedCount(alreadyExistedCount);
            result.setSucceededCount(recalculatedCount);
            result.setFailedCount(0);
            return result;
        }

        int succeededCount = 0;
        int attemptedCount = 0;
        List<Long> failedPropertyIds = new ArrayList<>();

        for (PropertyListDTO property : newPropertiesToCalculate) {
            Long propertyId = property.getPropertyId();

            if (limit != null && attemptedCount >= limit) {
                break;
            }
            attemptedCount++;

            try {
                SafetyRouteRequestDTO req = new SafetyRouteRequestDTO();
                req.setPropertyId(propertyId);
                req.setDestinationId(destinationId);
                req.setDestinationName(destName);
                req.setDestinationAddress(destination.getAddress());
                req.setDestinationLatitude(destLat);
                req.setDestinationLongitude(destLng);

                safetyService.getOrCalculateSafety(req);

                succeededCount++;
                log.info("운영 DB(property_safety & property_safety_route) 시드 저장 완료: propertyId={} ({}건째)", propertyId,
                        succeededCount);
            } catch (Exception exception) {
                failedPropertyIds.add(propertyId);
                log.warn("운영 DB 시드 저장 실패: propertyId={}, message={}", propertyId, exception.getMessage());
            }

            sleepQuietly();
        }

        TestLineStringSeedResultDTO result = new TestLineStringSeedResultDTO();
        result.setFetchedCount(fetchedCount);
        result.setAlreadyExistedCount(alreadyExistedCount);
        result.setSucceededCount(succeededCount);
        result.setFailedCount(failedPropertyIds.size());
        result.setFailedPropertyIds(failedPropertyIds);
        return result;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(CALL_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
