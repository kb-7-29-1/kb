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
     * 서울시 전역 25개 자치구 x 최근 3개월 실거래가 매물 전체 수집 및 Railway DB 벌크 동기화
     */
    public int syncAllSeoulRecent3Months() {
        log.info(
                "Starting Full Seoul (25 Districts x Recent 3 Months x 3 Building Types) Public Data API to Railway DB Bulk Sync...");
        List<String> recent3Months = getRecent3MonthsYmd();
        int totalInserted = 0;
        int[] buildingTypes = { 3 }; // 1: 연립다세대/빌라(주석처리), 2: 단독/다가구(주석처리), 3: 오피스텔만 먼저 수집

        List<PropertyListDTO> batchBuffer = new ArrayList<>();

        for (String lawdCd : SEOUL_LAWD_CODES) {
            for (String dealYmd : recent3Months) {
                for (int bType : buildingTypes) {
                    try {
                        List<PropertyListDTO> properties = publicDataApiService.fetchRealDataForBuildingType(bType,
                                lawdCd, dealYmd);
                        if (properties != null && !properties.isEmpty()) {
                            batchBuffer.addAll(properties);
                        }
                    } catch (Exception e) {
                        log.error("Failed batch fetch for bType: {}, LAWD_CD: {}, DEAL_YMD: {} - {}", bType, lawdCd,
                                dealYmd, e.getMessage());
                    }

                    // 버퍼가 100건 이상 쌓이면 Railway DB로 1회 벌크 저장전송 (네트워크 병목 95% 감소)
                    if (batchBuffer.size() >= 100) {
                        try {
                            int inserted = propertyMapper.insertBatchPublicProperties(batchBuffer);
                            totalInserted += inserted;
                            log.info("Bulk Inserted {} items into Railway DB (Cumulative: {})", inserted,
                                    totalInserted);
                        } catch (Exception e) {
                            log.error("Bulk insert to Railway DB error: {}", e.getMessage(), e);
                        }
                        batchBuffer.clear();
                    }
                }
            }
        }

        // 남은 버퍼 최종 일괄 저장
        if (!batchBuffer.isEmpty()) {
            try {
                int inserted = propertyMapper.insertBatchPublicProperties(batchBuffer);
                totalInserted += inserted;
                log.info("Final Bulk Inserted {} items into Railway DB.", inserted);
            } catch (Exception e) {
                log.error("Final bulk insert to Railway DB error: {}", e.getMessage(), e);
            }
            batchBuffer.clear();
        }

        log.info("Completed Railway DB Bulk Sync. Total Items: {}", totalInserted);
        return totalInserted;
    }

    /**
     * DB에 이미 수집된 매물들의 주소를 네이버 지오코딩 API로 정밀 위경도 일괄 갱신 (무제한 300만건 활용)
     */
    public int updateAllDbGeocodes() {
        log.info("Starting Full DB Geocode Update via Naver Geocoding...");
        List<PropertyListDTO> properties = propertyMapper.selectAllPropertiesToGeocode();
        if (properties == null || properties.isEmpty()) {
            log.info("No properties found in DB to geocode.");
            return 0;
        }

        log.info("Total Properties found in DB for Geocoding: {}", properties.size());
        int updatedCount = 0;

        for (PropertyListDTO p : properties) {
            if (p.getAddress() == null || p.getAddress().trim().isEmpty()) continue;

            double[] coords = publicDataApiService.getRealCoordinatesFromAddress(p.getAddress());
            if (coords != null) {
                try {
                    propertyMapper.updatePropertyCoordinates(p.getPropertyId(), coords[0], coords[1]);
                    updatedCount++;
                    if (updatedCount % 500 == 0) {
                        log.info("Geocoded & Updated {} / {} properties in DB.", updatedCount, properties.size());
                    }
                } catch (Exception e) {
                    log.error("Failed to update coordinates for propertyId {}: {}", p.getPropertyId(), e.getMessage());
                }
            }
        }

        log.info("Successfully Completed Full DB Geocoding! Total Updated Properties: {}", updatedCount);
        return updatedCount;
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
