package com.salgosipo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@EnableWebMvc
@ComponentScan(basePackages = {
        "com.salgosipo.controller",
        "com.salgosipo.exception"})
public class ServletConfig implements WebMvcConfigurer {
    //스프링 내부에서 사용하는 서블릿(jsp)와 관련된 설정하는 파일.

    //Servlet 3.0 이상 파일 업로드 사용시
    @Bean //메서드를 호출했을 때 싱글톤빈을 만들어 리턴해주세요.
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //jsp에서 사용할 프론트용 자원들(js, css, img위치와 접근 주소 설정)
        registry
                .addResourceHandler("/resources/**")
                // url이 /resources/로 시작하는 모든 경로
                .addResourceLocations("/resources/");
    }
    // webapp/resources/경로로 매핑

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        //서버의 결과를 넣을 jsp위치와 전체 경로를 결정
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/views/");
        bean.setSuffix(".jsp");
        registry.viewResolver(bean);
    }
}
