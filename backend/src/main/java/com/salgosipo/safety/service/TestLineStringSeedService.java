package com.salgosipo.safety.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertyPageResponseDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.service.PropertyService;
import com.salgosipo.safety.client.SafetyRouteClient;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.PropertySafetyVO;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.dto.SafetyRouteRequestDTO;
import com.salgosipo.safety.dto.TestLineStringSeedResultDTO;
import com.salgosipo.safety.mapper.SafetyMapper;
import com.salgosipo.safety.mapper.TestLineStringMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [임시/테스트 전용] 세종대 도보권 매물의 TMAP 보행자 경로(linestring)를
 * test_line_string 테이블에 저장해서 눈으로 검증하기 위한 배치 서비스입니다.
 *
 * TMAP API 일일 호출 한도(1,000회)를 지키기 위해, destinationId 기준으로
 * 이미 저장된 property_id 목록을 한 번에 조회해서 메모리에서 걸러내고,
 * 이미 저장된 매물은 절대 다시 TMAP을 호출하지 않습니다.
 *
 * 검증이 끝나면 이 클래스, TestLineStringController, TestLineStringMapper(+xml),
 * TestLineStringVO, TestLineStringSeedResultDTO, test_line_string 테이블은
 * 전부 삭제해도 됩니다.
 */
@Service
public class TestLineStringSeedService {

    private static final Logger log = LogManager.getLogger(TestLineStringSeedService.class);
    private static final int CALL_INTERVAL_MILLIS = 150;

    private final PropertyService propertyService;
    private final SafetyMapper safetyMapper;
    private final TestLineStringMapper testLineStringMapper;
    private final SafetyService safetyService;

    @Autowired
    public TestLineStringSeedService(
            PropertyService propertyService,
            SafetyMapper safetyMapper,
            TestLineStringMapper testLineStringMapper,
            SafetyService safetyService) {
        this.propertyService = propertyService;
        this.safetyMapper = safetyMapper;
        this.testLineStringMapper = testLineStringMapper;
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

        List<Long> allPropertyIds = properties.stream()
                .map(PropertyListDTO::getPropertyId)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> existingPropertyIds = new HashSet<>();

        // 1) 이전에 test_line_string 테이블에 수집해둔 ID 합산
        try {
            List<Long> testLineIds = testLineStringMapper.selectExistingPropertyIds(destinationId);
            if (testLineIds != null) {
                existingPropertyIds.addAll(testLineIds);
            }
        } catch (Exception ignored) {
        }

        // 2) 실제 운영 DB property_safety 테이블에 저장된 모든 ID 직접 합산 (SELECT * FROM
        // property_safety WHERE destination_id = X)
        try {
            List<PropertySafetyVO> prodList = safetyMapper.selectPropertySafetyBatch(null, destinationId);
            if (prodList != null) {
                prodList.stream()
                        .map(PropertySafetyVO::getPropertyId)
                        .filter(Objects::nonNull)
                        .forEach(existingPropertyIds::add);
            }
        } catch (Exception ignored) {
        }

        List<PropertyListDTO> newPropertiesToCalculate = properties.stream()
                .filter(p -> p.getPropertyId() != null && !existingPropertyIds.contains(p.getPropertyId()))
                .toList();

        int fetchedCount = properties.size();
        int alreadyExistedCount = fetchedCount - newPropertiesToCalculate.size();
        int toCalculateCount = newPropertiesToCalculate.size();

        log.info("===============================================================");
        log.info("[안전 시드 배치 사전 검증 결과]");
        log.info("- 목적지: {} (ID: {})", destName, destinationId);
        log.info("- 검색된 전체 매물: {}건", fetchedCount);
        log.info("- DB에 이미 저장된 매물 (TMAP 0건 스킵): {}건", alreadyExistedCount);
        log.info("- ⚡ 이번에 TMAP API를 호출할 신규 매물: {}건", toCalculateCount);
        log.info("===============================================================");

        if (toCalculateCount == 0) {
            log.info("모든 매물이 이미 DB에 저장되어 있어 TMAP API 호출 없이 종료합니다.");
            TestLineStringSeedResultDTO result = new TestLineStringSeedResultDTO();
            result.setFetchedCount(fetchedCount);
            result.setAlreadyExistedCount(alreadyExistedCount);
            result.setSucceededCount(0);
            result.setFailedCount(0);
            return result;
        }

        boolean confirmed = false;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        "목적지: " + destName + " (ID: " + destinationId + ")\n" +
                        "검색된 전체 매물: " + fetchedCount + "건\n" +
                        "DB에 이미 저장된 매물 (TMAP 0건 스킵): " + alreadyExistedCount + "건\n\n" +
                        "⚡ 이번에 TMAP API를 호출할 신규 매물: " + toCalculateCount + "건\n\n" +
                        "진짜 TMAP API를 호출하여 DB 시드를 실행하시겠습니까?",
                        "안전 시드 배치 실행 확인",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                confirmed = (choice == JOptionPane.YES_OPTION);
            }
        } catch (Exception e) {
            log.warn("GUI 팝업창 호출 중 예외 발생: {}", e.getMessage());
        }

        if (!confirmed) {
            log.warn("===============================================================");
            log.warn("[배치 실행 취소] 팝업 창에서 '예(Y)'를 누르지 않아 API 호출 및 DB 시드 저장이 안전하게 취소되었습니다.");
            log.warn("===============================================================");
            TestLineStringSeedResultDTO result = new TestLineStringSeedResultDTO();
            result.setFetchedCount(fetchedCount);
            result.setAlreadyExistedCount(alreadyExistedCount);
            result.setSucceededCount(0);
            result.setFailedCount(0);
            return result;
        }

        int succeededCount = 0;
        int attemptedCount = 0;
        List<Long> failedPropertyIds = new ArrayList<>();

        for (PropertyListDTO property : newPropertiesToCalculate) {
            Long propertyId = property.getPropertyId();

            // limit은 "이번 호출에서 TMAP을 몇 번까지 실제로 때릴지"를 제한합니다.
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

                // 운영 DB (property_safety 및 property_safety_route)에 안전점수 및 경로 저장
                safetyService.getOrCalculateSafety(req);

                succeededCount++;
                log.info("운영 DB(property_safety & property_safety_route) 시드 저장 완료: propertyId={} ({}건째)", propertyId,
                        succeededCount);
            } catch (Exception exception) {
                failedPropertyIds.add(propertyId);
                log.warn(
                        "운영 DB 시드 저장 실패: propertyId={}, message={}",
                        propertyId,
                        exception.getMessage());
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