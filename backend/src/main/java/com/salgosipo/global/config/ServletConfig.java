package com.salgosipo.global.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.util.List;

@EnableWebMvc
@ComponentScan(basePackages = {
        "com.salgosipo.amenity.controller",
        "com.salgosipo.auth.controller",
        "com.salgosipo.bookmark.controller",
        "com.salgosipo.comment.controller",
        "com.salgosipo.destination.controller",
        "com.salgosipo.onboarding.controller",
        "com.salgosipo.property.controller",
        "com.salgosipo.safety.controller",
        "com.salgosipo.user.controller",
        "com.salgosipo.global.exception"
})
public class ServletConfig implements WebMvcConfigurer {
    //스프링 내부에서 사용하는 서블릿(jsp)와 관련된 설정하는 파일.

    //Servlet 3.0 이상 파일 업로드 사용시
    @Bean //메서드를 호출했을 때 싱글톤빈을 만들어 리턴해주세요.
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/resources/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //jsp에서 사용할 프론트용 자원들(js, css, img위치와 접근 주소 설정)
        registry
                .addResourceHandler("/resources/**")
                // url이 /resources/로 시작하는 모든 경로
                .addResourceLocations("/resources/");

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/resources/assets/");
    }
    // webapp/resources/경로로 매핑

    // 날짜/시간 데이터를 JSON으로 변환
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.getObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

}
