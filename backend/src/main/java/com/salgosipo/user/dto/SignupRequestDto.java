package com.salgosipo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문/숫자만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글/영문만 입력할 수 있습니다.")
    private String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
    private String email;

    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일은 오늘 이전이어야 합니다.")
    private Date birthDate;

    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F여야 합니다.")
    private String gender;
}