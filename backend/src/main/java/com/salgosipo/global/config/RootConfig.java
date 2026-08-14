package com.salgosipo.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@PropertySource(value = { "classpath:/application.properties" }, ignoreResourceNotFound = true)
@MapperScan(basePackages = { "com.salgosipo.amenity.mapper",
        "com.salgosipo.auth.mapper",
        "com.salgosipo.bookmark.mapper",
        "com.salgosipo.comment.mapper",
        "com.salgosipo.destination.mapper",
        "com.salgosipo.onboarding.mapper",
        "com.salgosipo.property.mapper",
        "com.salgosipo.routevote.mapper",
        "com.salgosipo.safety.mapper",
        "com.salgosipo.user.mapper" })
@ComponentScan(basePackages = { "com.salgosipo.amenity.service",
        "com.salgosipo.auth.service",
        "com.salgosipo.bookmark.service",
        "com.salgosipo.comment.service",
        "com.salgosipo.destination.service",
        "com.salgosipo.onboarding.service",
        "com.salgosipo.property.service",
        "com.salgosipo.property.client",
        "com.salgosipo.routevote.service",
        "com.salgosipo.safety.service",
        "com.salgosipo.user.service",
        "com.salgosipo.loan.service",
        "com.salgosipo.loan.client",
        "com.salgosipo.amenity.client",
        "com.salgosipo.global.config" })
public class RootConfig {
    // 프로젝트 전체에서 사용할 중요한 싱글톤 빈 생성 정의
    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    @Value("${jdbc.driver:${JDBC_DRIVER:${jdbc_driver:net.sf.log4jdbc.sql.jdbcapi.DriverSpy}}}")
    String driver;

    @Value("${jdbc.url:${JDBC_URL:${jdbc_url:}}}")
    String url;

    @Value("${jdbc.username:${JDBC_USERNAME:${jdbc_username:}}}")
    String username;

    @Value("${jdbc.password:${JDBC_PASSWORD:${jdbc_password:}}}")
    String password;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(resolveValue(driver, "jdbc.driver", "JDBC_DRIVER", "jdbc_driver"));
        config.setJdbcUrl(resolveValue(url, "jdbc.url", "JDBC_URL", "jdbc_url"));
        config.setUsername(resolveValue(username, "jdbc.username", "JDBC_USERNAME", "jdbc_username"));
        config.setPassword(resolveValue(password, "jdbc.password", "JDBC_PASSWORD", "jdbc_password"));
        config.setConnectionInitSql("SET time_zone = '+09:00'");
        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    private String resolveValue(String injectedVal, String... fallbackKeys) {
        if (injectedVal != null && !injectedVal.isBlank() && !injectedVal.startsWith("${")) {
            return injectedVal.trim();
        }
        for (String key : fallbackKeys) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) return val.trim();
            val = System.getProperty(key);
            if (val != null && !val.isBlank()) return val.trim();
        }
        return injectedVal != null ? injectedVal.trim() : "";
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());
        return (SqlSessionFactory) sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
        return manager;
    }
}
