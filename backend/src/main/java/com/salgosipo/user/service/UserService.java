package com.salgosipo.user.service;

import com.salgosipo.user.domain.UserVO;
import com.salgosipo.user.dto.SignupRequestDto;
import com.salgosipo.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequestDto dto){
        if(userMapper.countByLoginId(dto.getLoginId())>0){
            throw new IllegalArgumentException("이미 사용중인 ID 입니다");
        }
        if (userMapper.countByEmail(dto.getEmail())>0){
            throw new IllegalArgumentException("이미 사용중인 EMAIL 입니다");
        }

        UserVO vo = UserVO.builder()
                .loginId(dto.getLoginId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .birthDate(dto.getBirthDate())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .build();

        userMapper.signup(vo);
    }

    public boolean isLoginIdAvailable(String loginId){
        return userMapper.countByLoginId(loginId)==0;
    }

    public boolean isEmailAvailable(String email){
        return userMapper.countByEmail(email)==0;
    }
}