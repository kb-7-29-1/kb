package com.salgosipo.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
    String findLoginIdByEmail(String email);

    Long findUserIdForPasswordReset(@Param("loginId") String loginId,
                                    @Param("name") String name,
                                    @Param("email") String email);

    void resetPassword(@Param("userId") Long userId, @Param("password") String password);
}
