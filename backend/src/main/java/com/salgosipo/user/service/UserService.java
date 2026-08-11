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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PROFILE_IMAGE_LENGTH = 3_000_000; // data URL 기준 대략 2MB 원본 이미지 상당

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

        try {
            userMapper.signup(vo);
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 같은 아이디로 두 번 가입 요청이 들어와 위 countByLoginId 체크를
            // 함께 통과한 경우, login_id UNIQUE 제약으로 두 번째 INSERT가 여기서 걸림
            throw new IllegalArgumentException("이미 사용중인 ID 입니다");
        }
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
        if(userMapper.countByEmailExcludingUser(dto.getEmail(), vo.getUserId())>0){
            throw new IllegalArgumentException("이미 사용중인 EMAIL 입니다");
        }
        try {
            userMapper.updateProfile(vo.getUserId(),dto.getName(),dto.getEmail());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 사용중인 EMAIL 입니다");
        }
    }

    @Transactional
    public void updateProfileImage(String loginId, String profileImage){
        UserVO vo = userMapper.findByLoginId(loginId);
        if(vo == null){
            throw new IllegalArgumentException("존재하지않는 사용자입니다.");
        }
        if(profileImage != null && profileImage.length() > MAX_PROFILE_IMAGE_LENGTH){
            throw new IllegalArgumentException("이미지 용량이 너무 큽니다.");
        }
        userMapper.updateProfileImage(vo.getUserId(), profileImage);
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