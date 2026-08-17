package com.salgosipo.global.routing;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * application.properties와 독립된 로컬 전용 라우팅 설정 파일(docker-routing.properties)을 로드합니다.
 */
@Slf4j
public class RoutingConfigLoader {

    private static final String CONFIG_FILE = "docker-routing.properties";
    private static final Properties props = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // 1. 파일시스템 (프로젝트 루트 또는 현재 실행 경로)
        Path localPath = Paths.get(CONFIG_FILE);
        if (Files.exists(localPath)) {
            try (InputStream in = Files.newInputStream(localPath)) {
                props.load(in);
                log.info("[RoutingConfigLoader] 로컬 파일({})에서 라우팅 설정 로드 완료", localPath.toAbsolutePath());
                return;
            } catch (Exception ignored) {
            }
        }

        // 2. 클래스패스 (src/main/resources/docker-routing.properties)
        try (InputStream in = RoutingConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
                log.info("[RoutingConfigLoader] 클래스패스({})에서 라우팅 설정 로드 완료", CONFIG_FILE);
            }
        } catch (Exception ignored) {
        }
    }

    public static String getEngineMode() {
        // 우선순위 1: 시스템 프로퍼티 / 환경변수
        String env = System.getenv("ROUTING_ENGINE_MODE");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String sysProp = System.getProperty("routing.engine.mode");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }

        // 우선순위 2: docker-routing.properties 파일
        String fileVal = props.getProperty("routing.engine.mode");
        if (fileVal != null && !fileVal.isBlank()) {
            return fileVal.trim();
        }

        return "DOCKER"; // 기본값
    }

    public static boolean isDockerMode() {
        return "DOCKER".equalsIgnoreCase(getEngineMode());
    }

    public static String getValhallaUrl() {
        String val = props.getProperty("routing.valhalla.url");
        return (val != null && !val.isBlank()) ? val.trim() : "http://localhost:8000";
    }

    public static String getGraphhopperUrl() {
        String val = props.getProperty("routing.graphhopper.url");
        return (val != null && !val.isBlank()) ? val.trim() : "http://localhost:8001";
    }
}
