package com.salgosipo.auth.service;

import com.salgosipo.auth.dto.FindIdResponseDto;
import com.salgosipo.auth.dto.FindPasswordRequestDto;
import com.salgosipo.auth.dto.ResetPasswordRequestDto;
import com.salgosipo.auth.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public FindIdResponseDto findId(String email){
        String loginId = authMapper.findLoginIdByEmail(email);
        if(loginId == null){
            throw new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다.");
        }
        return new FindIdResponseDto(maskLoginId(loginId));
    }

    public String maskLoginId(String loginId){
        if(loginId.length()<=3){
            return loginId.charAt(0)+ "**";
        }
        int visibleLength = loginId.length() / 2;
        return loginId.substring(0,visibleLength) + "*".repeat(loginId.length() - visibleLength);
    }

    public Long verifyForPasswordReset(FindPasswordRequestDto dto){
        Long userId = authMapper.findUserIdForPasswordReset(dto.getLoginId(), dto.getName(), dto.getEmail());

        if(userId == null){
            throw new IllegalArgumentException("일치하는 계정 정보가 없습니다.");
        }

        return userId;
    }

    public void resetPassword(ResetPasswordRequestDto dto){
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        authMapper.resetPassword(dto.getUserId(), encodedPassword);
    }
}