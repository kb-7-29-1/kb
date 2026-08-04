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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@PropertySource({"classpath:/application.properties"})
@MapperScan(basePackages  = {"com.salgosipo.amenity.mapper",
        "com.salgosipo.auth.mapper",
        "com.salgosipo.bookmark.mapper",
        "com.salgosipo.comment.mapper",
        "com.salgosipo.destination.mapper",
        "com.salgosipo.onboarding.mapper",
        "com.salgosipo.property.mapper",
        "com.salgosipo.safety.mapper",
        "com.salgosipo.user.mapper"})
@ComponentScan(basePackages = {"com.salgosipo.amenity.service",
        "com.salgosipo.auth.service",
        "com.salgosipo.bookmark.service",
        "com.salgosipo.comment.service",
        "com.salgosipo.destination.service",
        "com.salgosipo.onboarding.service",
        "com.salgosipo.property.service",
        "com.salgosipo.safety.service",
        "com.salgosipo.user.service",
        "com.salgosipo.loan.service",
        "com.salgosipo.loan.client"})
public class RootConfig {
    //프로젝트 전체에서 사용할 중요한 싱글톤 빈 생성 정의
    @Autowired
    ApplicationContext applicationContext;

    @Value("${jdbc.driver}")
    String driver;

    @Value("${jdbc.url}")
    String url;

    @Value("${jdbc.username}")
    String username;

    @Value("${jdbc.password}")
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
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());
        return (SqlSessionFactory) sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(){
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
        return manager;
    }
}
