package com.salgosipo.global.security.filter;

import lombok.extern.log4j.Log4j2;
import com.salgosipo.global.security.account.dto.LoginDTO;
import com.salgosipo.global.security.handler.LoginFailureHandler;
import com.salgosipo.global.security.handler.LoginSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Log4j2
@Component
public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    //부모생성자가 호출되어 먼저 만들어져야함. --> 부모생성자는 AuthenticationManager를 입력값으로 하는 생성자를 씀.
    //자식생성자에서 부모생성자를 가시성있게 호출해야함.
    public JwtUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,
                                                   LoginSuccessHandler loginSuccessHandler,
                                                   LoginFailureHandler loginFailureHandler){
        super(authenticationManager); //맨 첫줄에 써주어야함.

        //jwt~~필터 언제 적용할지 설정.
        setFilterProcessesUrl("/api/auth/login");
        setAuthenticationSuccessHandler(loginSuccessHandler);
        setAuthenticationFailureHandler(loginFailureHandler);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        //필터처리할 내용을 구현하면 됨.
        //http body로 전달할 json을 꺼내서 dto로 옮겨주어야함.
        LoginDTO login = LoginDTO.of(request);

        //인증매니저에게 id/pw 인증해달라고 요청(username/passwordtoken으로 만들어서 주어야함.)
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

        //인증매니저에게 username/passwordtoken을 주면서 인증해달라고 요청함.
        //인증이 완료가 되면 인증정보를 담고 있는 Authentication객체를 생성해서 리턴함.
        return getAuthenticationManager().authenticate(token);
    }
}
