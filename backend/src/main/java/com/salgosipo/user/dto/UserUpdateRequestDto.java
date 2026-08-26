package com.salgosipo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequestDto {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글/영문만 입력할 수 있습니다.")
    private String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
    private String email;
}