package com.salgosipo.safety.service;

import com.salgosipo.global.config.RootConfig;
import com.salgosipo.global.security.config.SecurityConfig;
import com.salgosipo.safety.dto.TestLineStringSeedResultDTO;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 시연용 4개 목적지(세종대, 중앙대, 연세대, 공릉역) 안전점수/경로 일괄 시드 배치 테스트
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class, SecurityConfig.class })
@Rollback(false)
@Commit
@Log4j2
public class SafetySeedBatchTest {

    static {
        System.setProperty("java.awt.headless", "false");
    }

    @Autowired
    private SafetySeedBatchService safetySeedBatchService;

    /**
     * 1. 세종대학교 (destinationId = 1)
     * 실행: cd backend; .\gradlew test --tests com.salgosipo.safety.service.SafetySeedBatchTest.seedSejongTest
     */
    @Test
    public void seedSejongTest() {
        log.info("Starting Sejong Univ Safety Seed (ID: 1)...");
        TestLineStringSeedResultDTO result = safetySeedBatchService.seed(1, 1.2);
        log.info("Sejong Seed Result: {}", result);
    }

    /**
     * 2. 중앙대학교 (destinationId = 126)
     * 실행: cd backend; .\gradlew test --tests com.salgosipo.safety.service.SafetySeedBatchTest.seedChungangTest
     */
    @Test
    public void seedChungangTest() {
        log.info("Starting Chung-Ang Univ Safety Seed (ID: 126)...");
        TestLineStringSeedResultDTO result = safetySeedBatchService.seed(126, 1.2);
        log.info("Chung-Ang Seed Result: {}", result);
    }

    /**
     * 3. 연세대학교 (destinationId = 5)
     * 실행: cd backend; .\gradlew test --tests com.salgosipo.safety.service.SafetySeedBatchTest.seedYonseiTest
     */
    @Test
    public void seedYonseiTest() {
        log.info("Starting Yonsei Univ Safety Seed (ID: 5)...");
        TestLineStringSeedResultDTO result = safetySeedBatchService.seed(5, 1.2);
        log.info("Yonsei Seed Result: {}", result);
    }

    /**
     * 4. 공릉역 7호선 (destinationId = 1234)
     * 실행: cd backend; .\gradlew test --tests com.salgosipo.safety.service.SafetySeedBatchTest.seedGongneungTest
     */
    @Test
    public void seedGongneungTest() {
        log.info("Starting Gongneung Stn Safety Seed (ID: 1234)...");
        TestLineStringSeedResultDTO result = safetySeedBatchService.seed(1234, 1.2);
        log.info("Gongneung Seed Result: {}", result);
    }
}
