package com.salgosipo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {
    private Long userId;
    private String loginId;
    private String password;
    private String name;
    private Date birthDate;
    private String email;
    private String gender;
}