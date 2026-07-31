package com.salgosipo.global.security.account.dto;

import com.salgosipo.user.domain.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserInfoDTO {
    String loginId;
    String name;
    String email;

    public static UserInfoDTO of(UserVO user) {
        return new UserInfoDTO(
                user.getLoginId(), user.getName(), user.getEmail()
        );
    }

}
