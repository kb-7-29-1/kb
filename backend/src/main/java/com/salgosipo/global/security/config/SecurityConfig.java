package com.salgosipo.global.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import com.salgosipo.global.security.filter.AuthenticationErrorFilter;
import com.salgosipo.global.security.filter.JwtAuthenticationFilter;
import com.salgosipo.global.security.filter.JwtUsernamePasswordAuthenticationFilter;
import com.salgosipo.global.security.handler.CustomAccessDeniedHandler;
import com.salgosipo.global.security.handler.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@Log4j2
@MapperScan(basePackages = {"com.salgosipo.global.security.account.mapper"})
@ComponentScan(basePackages  = {"com.salgosipo.global.security"})
@RequiredArgsConstructor
public class SecurityConfig  extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationErrorFilter authenticationErrorFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 문자셋필터 ==> post방식으로 전달된 데이터 utf-8로 설정해서 받아야 한글깨지지 않음.
    // http의 body에 따라오므로 별도로 설정해주어야함.
    // ip, pw를 입력에 한글 깨지지 않게 설정, 모든 회원가입, 로그인은  post로 보내야함.
    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return encodingFilter;
    }

    @Bean
//    JWT 방식은 폼로그인과달리 Spring Security의
//    기본인증필터를사용하지않고,
//    클라이언트→ JWT 토큰→ 커스텀필터
//            (OncePerRequestFilter 등) → SecurityContext 직접 설정
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    //시큐리티 적용하고 싶지 않은 요청 주소 등록.
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/*", "/api/member/**", "/swagger-ui.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(encodingFilter(),
                        CsrfFilter.class)
                .addFilterBefore(authenticationErrorFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                    jwtUsernamePasswordAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        http
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);

        http.httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);



        http
                .authorizeRequests() // 경로별 접근 권한 설정
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers(HttpMethod.POST,"/api/member//role별 접근 권한 설정\n" +
                        "//        http.authorizeRequests()\n" +
                        "//                .antMatchers(\"/security/all\").permitAll()\n" +
                        "//                .antMatchers(\"/security/admin\").access(\"hasRole('ROLE_ADMIN')\")\n" +
                        "//                .antMatchers(\"/security/member\").access(\"hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')\");").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/member", "/api/member/*/changepassword").authenticated()
                // 게시판
                .antMatchers(HttpMethod.GET, "/api/board/download/**").authenticated()
                .antMatchers(HttpMethod.POST, "/api/board/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/board/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/api/board/**").authenticated()
                .anyRequest().permitAll(); // 일단 모든 접근 허용
//                .antMatchers("/api/security/all").permitAll() // 모두 허용
//                .antMatchers("/api/security/member").access("hasRole('ROLE_MEMBER')") // ROLE_MEMBER 이상 접근 허용
//                .antMatchers("/api/security/admin").access("hasRole('ROLE_ADMIN')")
//                .anyRequest().authenticated();

        // 기본 로그인 화면 다시 활성화
        // http.formLogin();

        // 커스텀한 로그인 화면 설정
        http.formLogin()
                .loginPage("/security/login")
                .loginProcessingUrl("/security/login")
                .defaultSuccessUrl("/");

        http.logout() // 로그아웃설정시작
                .logoutUrl("/security/logout") // POST: 로그아웃 호출 url
                .invalidateHttpSession(true) // 세션 invalidate
                .deleteCookies("remember-me", "JSESSION-ID") // 삭제할 쿠키 목록
                .logoutSuccessUrl("/security/logout"); // GET: 로그아웃 이후이동할페이지


    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        log.info("개인 계정 설정- ram에 만들어 테스트(in memory)");

        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

//        auth.inMemoryAuthentication()
//                .withUser("admin")
////                .password("{noop}1234")
//                .password("$2a$10$CJEer.FckE2NtlHzgKRShemk8iSXL0JLPewdAQYLqDRXjG/PvKan6")
//                .roles("ADMIN", "MEMBER");
//
//        auth.inMemoryAuthentication()
//                .withUser("member")
////                .password("{noop}1234")
//                .password("$2a$10$XyJq1p6ZfcjhFNcYj253yOyAlvcVqeAbcbGOubKAnS1sHkNAzB/Wq")
//                .roles("MEMBER");

    }
}
