package com.salgosipo.global.routing;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * application.properties와 독립된 로컬 전용 라우팅 설정 파일(docker-routing.properties)을
 * 로드합니다.
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
        return resolvePlaceholder(props.getProperty("routing.engine.mode"), "ROUTING_ENGINE_MODE", "DOCKER");
    }

    public static boolean isDockerMode() {
        return "DOCKER".equalsIgnoreCase(getEngineMode());
    }

    public static String getValhallaUrl() {
        return resolvePlaceholder(props.getProperty("routing.valhalla.url"), "ROUTING_VALHALLA_URL", "http://localhost:8000");
    }

    public static String getMotisUrl() {
        return resolvePlaceholder(props.getProperty("routing.motis.url"), "ROUTING_MOTIS_URL", "http://localhost:8001");
    }

    public static String getGraphhopperUrl() {
        return resolvePlaceholder(props.getProperty("routing.graphhopper.url"), "ROUTING_GRAPHHOPPER_URL", "http://localhost:8001");
    }

    /**
     * 환경변수, 시스템 프로퍼티, Spring 템플릿 문법(${KEY:DEFAULT})을 100% 자동 파싱하여 안전하게 반환
     */
    private static String resolvePlaceholder(String val, String envKey, String fallback) {
        // 1. OS 환경변수 1순위 (Railway 배포 환경)
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        // 2. JVM System Property 2순위 (-Drouting.engine.mode=...)
        String sys = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }

        // 3. properties 파일 값 파싱
        if (val == null || val.isBlank()) {
            return fallback;
        }
        val = val.trim();

        // 4. Spring 템플릿 문법 지원: ${ENV_NAME:DEFAULT_VALUE} -> DEFAULT_VALUE 추출
        if (val.startsWith("${") && val.contains(":")) {
            int colonIdx = val.indexOf(":");
            return val.substring(colonIdx + 1).replace("}", "").trim();
        }
        return val;
    }
}
