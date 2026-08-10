package com.salgosipo.global.security.account.dto;

import com.salgosipo.user.domain.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserInfoDTO {
    Long userId;
    String loginId;
    String name;
    String email;
    Date birthDate;

    public static UserInfoDTO of(UserVO user) {
        return new UserInfoDTO(
                user.getUserId(), user.getLoginId(), user.getName(), user.getEmail(), user.getBirthDate()
        );
    }

}
