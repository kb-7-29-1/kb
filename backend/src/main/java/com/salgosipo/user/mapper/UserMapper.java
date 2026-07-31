package com.salgosipo.user.mapper;

import com.salgosipo.user.domain.UserVO;
import com.salgosipo.user.dto.UserProfileResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserVO findByLoginId(String loginId);

    void signup(UserVO user);

    int countByLoginId(String loginId);

    int countByEmail(String email);

    UserProfileResponseDto findProfileByUserId(Long userId);

    void updateProfile(@Param("userId") Long userId,
                       @Param("name") String name,
                       @Param("email") String email);
}
