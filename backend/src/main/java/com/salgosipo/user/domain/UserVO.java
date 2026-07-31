package com.salgosipo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {
    private Long userId;
    private String loginId;
    private String password;
    private String name;
    private String birthDate;
    private String gender;
    private String email;
    private String delYn;
}