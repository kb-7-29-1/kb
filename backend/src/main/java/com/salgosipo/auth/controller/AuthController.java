package com.salgosipo.auth.controller;

import com.salgosipo.auth.dto.FindIdRequestDto;
import com.salgosipo.auth.dto.FindIdResponseDto;
import com.salgosipo.auth.dto.FindPasswordRequestDto;
import com.salgosipo.auth.dto.ResetPasswordRequestDto;
import com.salgosipo.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody FindIdRequestDto dto){
        FindIdResponseDto result = authService.findId(dto.getEmail());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/find-password")
    public ResponseEntity<?> verifyForPasswordReset(@RequestBody FindPasswordRequestDto dto){
        String resetToken = authService.verifyForPasswordReset(dto);
        return ResponseEntity.ok(resetToken);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto dto){
        authService.resetPassword(dto);
        return ResponseEntity.ok().build();
    }
}