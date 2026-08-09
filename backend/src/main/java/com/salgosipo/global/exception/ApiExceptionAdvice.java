package com.salgosipo.global.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(2)
@Log4j2
public class ApiExceptionAdvice {

    //404, 500 어노테이션별로 메서드 정의해서 설정
    // 존재하지 않는 URL 요청인 경우
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("요청한 API 주소가 존재하지 않습니다.");

    }

    // @Valid 검증 실패 (길이/형식 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(message);
    }
    // 그 외 서버 내부 오류
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity

                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("서버 내부 오류가 발생했습니다." + e.getMessage());

    }
}
