package com.salgosipo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageUpdateRequestDto {
    // "data:image/jpeg;base64,..." 형태의 data URL. null이면 기본 아이콘으로 되돌림.
    private String image;
}