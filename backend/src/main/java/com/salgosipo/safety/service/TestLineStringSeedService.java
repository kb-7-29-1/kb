package com.salgosipo.safety.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.dto.PropertyPageResponseDTO;
import com.salgosipo.property.dto.PropertySearchCondDTO;
import com.salgosipo.property.service.PropertyService;
import com.salgosipo.safety.client.SafetyRouteClient;
import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.SafetyDestinationVO;
import com.salgosipo.safety.domain.TestLineStringVO;
import com.salgosipo.safety.dto.TestLineStringSeedResultDTO;
import com.salgosipo.safety.mapper.SafetyMapper;
import com.salgosipo.safety.mapper.TestLineStringMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final SafetyRouteClient safetyRouteClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public TestLineStringSeedService(
            PropertyService propertyService,
            SafetyMapper safetyMapper,
            TestLineStringMapper testLineStringMapper,
            @Value("${TMAP_API_KEY:}") String tmapApiKey
    ) {
        this.propertyService = propertyService;
        this.safetyMapper = safetyMapper;
        this.testLineStringMapper = testLineStringMapper;
        this.safetyRouteClient = new SafetyRouteClient(tmapApiKey);
    }

    public TestLineStringSeedResultDTO seed(
            Integer destinationId,
            Double lat,
            Double lng,
            Double radius,
            Integer maxDeposit,
            Long userId,
            Integer limit
    ) {
        SafetyDestinationVO destination = safetyMapper.selectDestinationById(destinationId);
        if (destination == null) {
            throw new IllegalArgumentException("존재하지 않는 destinationId입니다: " + destinationId);
        }
        double destLat = destination.getLatitude().doubleValue();
        double destLng = destination.getLongitude().doubleValue();
        String destName = destination.getName();

        PropertySearchCondDTO cond = PropertySearchCondDTO.builder()
                .destinationId(destinationId)
                .lat(lat)
                .lng(lng)
                .radius(radius)
                .maxDeposit(maxDeposit)
                .page(1)
                .size(600)
                .build();

        PropertyPageResponseDTO page = propertyService.getPropertyList(cond, userId);
        List<PropertyListDTO> properties = page.getItems() == null ? List.of() : page.getItems();

        Set<Long> existingPropertyIds = new HashSet<>(
                testLineStringMapper.selectExistingPropertyIds(destinationId)
        );

        int fetchedCount = properties.size();
        int alreadyExistedCount = 0;
        int succeededCount = 0;
        int attemptedCount = 0;
        List<Long> failedPropertyIds = new ArrayList<>();

        for (PropertyListDTO property : properties) {
            Long propertyId = property.getPropertyId();

            if (existingPropertyIds.contains(propertyId)) {
                alreadyExistedCount++;
                continue;
            }

            // limit은 "이번 호출에서 TMAP을 몇 번까지 실제로 때릴지"를 제한합니다.
            // 성공/실패 여부와 무관하게 시도 횟수 자체를 막아야 안전합니다.
            if (limit != null && attemptedCount >= limit) {
                break;
            }
            attemptedCount++;

            try {
                PedestrianRoute route = safetyRouteClient.findPreferredRoute(
                        property.getLatitude(),
                        property.getLongitude(),
                        property.getAddress(),
                        destLat,
                        destLng,
                        destName
                );

                TestLineStringVO vo = new TestLineStringVO();
                vo.setPropertyId(propertyId);
                vo.setDestinationId(destinationId);
                vo.setLineStringJson(objectMapper.writeValueAsString(route.getRoutePoints()));
                testLineStringMapper.insertTestLineString(vo);

                existingPropertyIds.add(propertyId);
                succeededCount++;
                log.info("test_line_string 저장 완료: propertyId={} ({}건째)", propertyId, succeededCount);
            } catch (Exception exception) {
                failedPropertyIds.add(propertyId);
                log.warn(
                        "test_line_string 저장 실패: propertyId={}, message={}",
                        propertyId,
                        exception.getMessage()
                );
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