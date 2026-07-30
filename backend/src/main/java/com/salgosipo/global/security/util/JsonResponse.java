package com.salgosipo.global.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

//브라우저에 보내는 역할의 클래스
//인증성공, 인증실패시 보내는 기능만 가짐.
public class JsonResponse {

    public static <T> void send(HttpServletResponse response, T result) throws IOException {
        ObjectMapper om = new ObjectMapper();
        response.setContentType("application/json;charset=UTF-8");
        Writer out = response.getWriter();
        out.write(om.writeValueAsString(result));
        out.flush(); //http의 body(json)으로 전송됨.
    }
    public static void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        Writer out = response.getWriter();
        out.write(message);
        out.flush(); //tcp전송, 보낼 데이터를 byte배열에 넣어서 보냄.
        //배열은 고정길이(256)
    }
}
