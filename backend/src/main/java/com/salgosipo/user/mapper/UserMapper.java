package com.salgosipo.user.mapper;

import com.salgosipo.user.domain.UserVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    UserVO findByLoginId(String loginId);

    void signup(UserVO user);

    int countByLoginId(String loginId);

    int countByEmail(String email);
}
