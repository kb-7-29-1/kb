package com.salgosipo.global.security.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.salgosipo.global.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtProcessor jwtProcessor;
    private final UserDetailsService userDetailsService;

    public Authentication  getAuthentication(String token) {
        String username = jwtProcessor.getUsername(token); //유효성검증 --> 로그인한 username
        UserDetails princiapl = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(princiapl, null, princiapl.getAuthorities());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //authorization이라고 붙어있는 header가 있는지 보고 값을 추출.
        //값에서 Bearer공백 뒤에 있는 jwt를 추출.
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)){
            String token = bearerToken.substring(BEARER_PREFIX.length()); //7인덱스이후의 문자열 추출

            Authentication authentication = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        //jwt유효성검증 --> username추출.
        //username으로 db에서 가장 최신의 정보를 꺼내온다.
        //securitycontext에 db에 꺼내온 정보는 넣어둔다.
        //Authentication인터페이스를 따르는 객체형태로 SpringSecurity의 securitycontext에 저장해둠.
        super.doFilter(request, response, filterChain);//다음 필터 연결
    }
}
