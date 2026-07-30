package com.salgosipo.global.security.account.mapper;

import org.scoula.security.account.domain.MemberVO;

public interface UserDetailsMapper {
    MemberVO get(String username);
}
