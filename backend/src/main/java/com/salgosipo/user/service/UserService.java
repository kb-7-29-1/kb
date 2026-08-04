package com.salgosipo.user.service;

import com.salgosipo.comment.mapper.CommentMapper;
import com.salgosipo.user.domain.UserVO;
import com.salgosipo.user.dto.PasswordChangeRequestDto;
import com.salgosipo.user.dto.SignupRequestDto;
import com.salgosipo.user.dto.UserProfileResponseDto;
import com.salgosipo.user.dto.UserUpdateRequestDto;
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
    private final CommentMapper commentMapper;
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

    public UserProfileResponseDto getProfile(String loginId){
        UserVO vo = userMapper.findByLoginId(loginId);
        if(vo == null){
            throw new IllegalArgumentException("존재하지않는 사용자입니다.");
        }
        return userMapper.findProfileByUserId(vo.getUserId());
    }

    @Transactional
    public void updateProfile(String loginId, UserUpdateRequestDto dto){
        UserVO vo = userMapper.findByLoginId(loginId);
        if(vo == null){
            throw new IllegalArgumentException("존재하지않는 사용자입니다.");
        }
        userMapper.updateProfile(vo.getUserId(),dto.getName(),dto.getEmail());
    }

    @Transactional
    public void changePassword(String loginId, PasswordChangeRequestDto dto){
        UserVO vo = userMapper.findByLoginId(loginId);
        if(vo == null){
            throw new IllegalArgumentException("존재하지않는 사용자입니다.");
        }
        if(!passwordEncoder.matches(dto.getCurrentPassword(),vo.getPassword())){
            throw new IllegalArgumentException("현재 비밀번호가 일치하지않습니다");
        }
        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        userMapper.updatePassword(vo.getUserId(), encodedNewPassword);
    }

    @Transactional
    public void withdraw(String loginId, String password){
        UserVO vo = userMapper.findByLoginId(loginId);
        if(vo == null){
            throw new IllegalArgumentException("존재하지않는 사용자입니다.");
        }
        if (!passwordEncoder.matches(password, vo.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        commentMapper.softDeleteByUserId(vo.getUserId());
        userMapper.withdraw(vo.getUserId());
    }
}