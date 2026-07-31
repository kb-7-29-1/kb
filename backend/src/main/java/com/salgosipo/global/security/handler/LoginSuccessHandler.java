package com.salgosipo.global.security.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.salgosipo.global.security.account.domain.CustomUser;
import com.salgosipo.global.security.account.dto.AuthResultDTO;
import com.salgosipo.global.security.account.dto.UserInfoDTO;
import com.salgosipo.global.security.util.JsonResponse;
import com.salgosipo.global.security.util.JwtProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProcessor jwtProcessor;

    private AuthResultDTO makeAuthResult(CustomUser user){
        String loginId = user.getUser().getLoginId();
        String token = jwtProcessor.generateToken(loginId);
        return new AuthResultDTO(token, UserInfoDTO.of(user.getUser()));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        //인증한 username의 db에서 가지고 온 정보를 UserInfoDTO로 만들고,
        //인증한 username의 정보를 jwt로 만든 것
        //AuthResultDTO에 모아야 함. --> 별도의 메서드를 만들자.
        // --> JsonResponse의 send(dto)를 호출하면 됨.

        //로그인한 사람의 정보인 CustomUser를 가지고 와야함.
        //이 정보를 꺼내올 수 있는 객체가 Authentication임.
        CustomUser user = (CustomUser)authentication.getPrincipal();
        AuthResultDTO result = makeAuthResult(user);
        JsonResponse.send(response, result);
    }
}
