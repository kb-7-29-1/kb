package com.salgosipo.property.service;

import com.salgosipo.global.config.RootConfig;
import com.salgosipo.global.security.config.SecurityConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class, SecurityConfig.class })
@Rollback(false)
@Commit
@Log4j2
public class PublicDataSyncServiceTest {

    @Autowired
    private PublicDataSyncService publicDataSyncService;

    /**
     * 서울 전역 25개 자치구 x 최근 3개월 실거래 매물 3종 세트 수동 수집 배치 실행 테스트
     * 실행 방법: cd backend; .\gradlew test --tests
     * com.salgosipo.property.service.PublicDataSyncServiceTest.manualBatchTest
     */
    @Test
    public void manualBatchTest() {
        log.info("Starting manual batch test for all 25 Seoul districts (recent 3 months)...");
        int totalInserted = publicDataSyncService.syncAllSeoulRecent3Months();
        log.info("Manual Batch Test Completed! Total Synced Properties: {}", totalInserted);
    }

    /**
     * DB 전체 기존 오피스텔 매물 주소를 네이버 지오코딩 API로 100% 정밀 GPS 위경도 일괄 갱신
     * 실행 방법: cd backend; .\gradlew test --tests com.salgosipo.property.service.PublicDataSyncServiceTest.updateGeocodesTest
     */
    @Test
    public void updateGeocodesTest() {
        log.info("Starting Full DB Geocodes Update Test...");
        int totalUpdated = publicDataSyncService.updateAllDbGeocodes();
        log.info("Full DB Geocodes Update Completed! Total Updated Properties: {}", totalUpdated);
    }
}
