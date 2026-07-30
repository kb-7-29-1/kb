package com.salgosipo.global.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.salgosipo.security.account.domain.MemberVO;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserInfoDTO {
    String username;
    String email;
    List<String> roles;

    public static UserInfoDTO of(MemberVO member) {
        return new UserInfoDTO(
                member.getUsername(),
                member.getEmail(),
                member.getAuthList().stream().map(a -> a.getAuth()).toList() // {"role_admin", "role_member"}
        );
    }

}
