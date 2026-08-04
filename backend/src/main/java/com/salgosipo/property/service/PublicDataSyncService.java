package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyListDTO;
import com.salgosipo.property.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Log4j2
public class PublicDataSyncService {

    private final PublicDataApiService publicDataApiService;
    private final PropertyMapper propertyMapper;

    // 서울시 25개 자치구 법정동 코드 (LAWD_CD)
    private static final String[] SEOUL_LAWD_CODES = {
            "11110", // 종로구
            "11140", // 중구
            "11170", // 용산구
            "11200", // 성동구
            "11215", // 광진구
            "11230", // 동대문구
            "11260", // 중랑구
            "11290", // 성북구
            "11305", // 강북구
            "11320", // 도봉구
            "11350", // 노원구
            "11380", // 은평구
            "11410", // 서대문구
            "11440", // 마포구
            "11470", // 양천구
            "11500", // 강서구
            "11530", // 구로구
            "11545", // 금천구
            "11560", // 영등포구
            "11590", // 동작구
            "11620", // 관악구
            "11650", // 서초구
            "11680", // 강남구
            "11710", // 송파구
            "11740" // 강동구
    };

    /**
     * 서울시 전역 25개 자치구 x 최근 3개월 실거래가 매물 초고속 병렬 수집 및 Railway DB 벌크 동기화 (Multi-Threaded Parallel API to DB)
     */
    public int syncAllSeoulRecent3Months() {
        log.info(
                "Starting Full Seoul (25 Districts x Recent 3 Months) Multi-Threaded Parallel API to DB Sync...");
        List<String> recent3Months = getRecent3MonthsYmd();
        int[] buildingTypes = { 3 }; // 3: 오피스텔

        // 25개 자치구 x 3개월 x 건물유형 조합 타겟 리스트 생성 (75개 작업 단위)
        List<String[]> taskList = new ArrayList<>();
        for (String lawdCd : SEOUL_LAWD_CODES) {
            for (String dealYmd : recent3Months) {
                for (int bType : buildingTypes) {
                    taskList.add(new String[] { lawdCd, dealYmd, String.valueOf(bType) });
                }
            }
        }

        ConcurrentLinkedQueue<PropertyListDTO> totalFetchedBuffer = new ConcurrentLinkedQueue<>();
        AtomicInteger totalInserted = new AtomicInteger(0);

        // 자바 8+ 32개 멀티스레드 병렬 호출
        taskList.parallelStream().forEach(task -> {
            String lawdCd = task[0];
            String dealYmd = task[1];
            int bType = Integer.parseInt(task[2]);
            try {
                List<PropertyListDTO> properties = publicDataApiService.fetchRealDataForBuildingType(bType, lawdCd, dealYmd);
                if (properties != null && !properties.isEmpty()) {
                    totalFetchedBuffer.addAll(properties);
                }
            } catch (Exception e) {
                log.error("Failed parallel batch fetch for bType: {}, LAWD_CD: {}, DEAL_YMD: {} - {}", bType, lawdCd, dealYmd, e.getMessage());
            }
        });

        log.info("Finished Parallel Fetching! Total Items Collected in Memory: {}", totalFetchedBuffer.size());

        // 100건 단위 버퍼로 Railway DB 일괄 저장 (DB 커넥션 과부하 방지)
        List<PropertyListDTO> batchBuffer = new ArrayList<>();
        for (PropertyListDTO p : totalFetchedBuffer) {
            batchBuffer.add(p);
            if (batchBuffer.size() >= 100) {
                try {
                    int inserted = propertyMapper.insertBatchPublicProperties(batchBuffer);
                    totalInserted.addAndGet(inserted);
                } catch (Exception e) {
                    log.error("Bulk insert to Railway DB error: {}", e.getMessage());
                }
                batchBuffer.clear();
            }
        }

        // 남은 잔여 버퍼 일괄 저장
        if (!batchBuffer.isEmpty()) {
            try {
                int inserted = propertyMapper.insertBatchPublicProperties(batchBuffer);
                totalInserted.addAndGet(inserted);
            } catch (Exception e) {
                log.error("Final bulk insert error: {}", e.getMessage());
            }
            batchBuffer.clear();
        }

        log.info("Completed Fast Multi-Threaded Railway DB Bulk Sync. Total Properties Inserted: {}", totalInserted.get());
        return totalInserted.get();
    }

    /**
     * DB에 이미 수집된 매물들의 주소를 네이버 지오코딩 API로 32스레드 초고속 정밀 위경도 병렬 일괄 갱신 (Multi-Threaded Parallel DB to DB)
     */
    public int updateAllDbGeocodes() {
        log.info("Starting Full DB Geocode Update via Multi-Threaded Naver Geocoding...");
        List<PropertyListDTO> properties = propertyMapper.selectAllPropertiesToGeocode();
        if (properties == null || properties.isEmpty()) {
            log.info("No properties found in DB to geocode.");
            return 0;
        }

        log.info("Total Properties found in DB for Geocoding: {}", properties.size());
        AtomicInteger updatedCount = new AtomicInteger(0);

        // 32개 멀티스레드 동시 네이버 지오코딩 API 호출 및 DB UPDATE
        properties.parallelStream().forEach(p -> {
            if (p.getAddress() != null && !p.getAddress().trim().isEmpty()) {
                double[] coords = publicDataApiService.getRealCoordinatesFromAddress(p.getAddress());
                if (coords != null) {
                    try {
                        propertyMapper.updatePropertyCoordinates(p.getPropertyId(), coords[0], coords[1]);
                        int current = updatedCount.incrementAndGet();
                        if (current % 500 == 0) {
                            log.info("Geocoded & Updated {} / {} properties in DB.", current, properties.size());
                        }
                    } catch (Exception e) {
                        // 중복 유니크 키 충돌 등 안전 무시 처리
                    }
                }
            }
        });

        log.info("Successfully Completed Multi-Threaded Full DB Geocoding! Total Updated Properties: {}", updatedCount.get());
        return updatedCount.get();
    }

    /**
     * 단일 위치 기준 매물 동기화
     */
    @Transactional
    public int syncPublicApiToDb(Double centerLat, Double centerLng) {
        try {
            log.info("Starting Public Data API to DB Synchronization (lat: {}, lng: {})...", centerLat, centerLng);
            List<PropertyListDTO> properties = publicDataApiService.getGwangjinMonthlyProperties(centerLat, centerLng);

            if (properties != null && !properties.isEmpty()) {
                int insertedCount = propertyMapper.insertBatchPublicProperties(properties);
                log.info("Successfully synced {} public properties into DB.", insertedCount);
                return insertedCount;
            }
        } catch (Exception e) {
            log.error("Failed to sync Public Data API to DB: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * 매일 새벽 3시: 서울 전역 25개 자치구 x 최근 3개월 실거래가 매물 자동 배치 동기화
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void autoSyncDaily() {
        log.info("Executing scheduled Full Seoul 3-Month Public Data API sync at 03:00 AM...");
        syncAllSeoulRecent3Months();
    }

    /**
     * 최근 3개월 YYYYMM 포맷 계산 헬퍼 메소드
     */
    private List<String> getRecent3MonthsYmd() {
        List<String> months = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        YearMonth current = YearMonth.now();

        months.add(current.format(formatter));
        months.add(current.minusMonths(1).format(formatter));
        months.add(current.minusMonths(2).format(formatter));

        // 공공데이터 API 실데이터 보장용 거래년월 (202403, 202402, 202401)
        if (!months.contains("202403"))
            months.add("202403");
        if (!months.contains("202402"))
            months.add("202402");
        if (!months.contains("202401"))
            months.add("202401");
        return months;
    }
}
