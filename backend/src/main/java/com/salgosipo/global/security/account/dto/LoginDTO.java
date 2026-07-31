package com.salgosipo.global.security.account.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginDTO {
    //form을 안쓰고 id,pw를 json으로 가지고 오려고 함.
    //json --> dto로 받아서 저장하려고 함.

    private String loginId;
    private String password;

    //http body로 전송된 데이터를 dto에 넣으면 됨.
    public static LoginDTO of(HttpServletRequest request) {
        ObjectMapper om = new ObjectMapper();
        //브라우저에는 java dto못보냄. --> json으로 변경해서 브라우저에서 읽을 수 있는 형태로 보냄.
        // --> 직렬화
        //브라우저는 json으로 데이터표현.. --> java dto로 변경해주어야함.
        // --> 역직렬화
        try {
            //request.getInputStream() : http body(json)을 읽어온다
            return om.readValue(request.getInputStream(), LoginDTO.class);
        } catch (IOException e) {
            throw new BadCredentialsException("json에 loginId, password가 없었음.");
        }
    }
}
