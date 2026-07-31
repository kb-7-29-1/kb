package com.salgosipo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {
    private String loginId;
    private String password;
    private String name;
    private String email;
    private Date birthDate;
    private String gender;
}