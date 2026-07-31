package com.salgosipo.global.security.account.domain;

import com.salgosipo.user.domain.UserVO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
@Setter
public class CustomUser extends User {
    private UserVO user;		// 실질적인 사용자 데이터

    public CustomUser(UserVO vo) {
        super(vo.getLoginId(), vo.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.user = vo;
    }
}
