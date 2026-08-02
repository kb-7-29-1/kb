package com.salgosipo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindPasswordRequestDto {
    private String loginId;
    private String name;
    private String email;
}
