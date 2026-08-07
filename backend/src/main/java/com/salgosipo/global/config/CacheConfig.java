package com.salgosipo.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("propertyList", "propertyDetail");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(600, TimeUnit.MINUTES) // 10시간 이후 자동 만료
                .maximumSize(10000) // 최대 10,000개 검색 조합/상세 정보 메모리 캐싱
                .recordStats());
        return cacheManager;
    }
    // 메모리 점유율은 약 30MB 내외 정도 예상, 가볍다
}
