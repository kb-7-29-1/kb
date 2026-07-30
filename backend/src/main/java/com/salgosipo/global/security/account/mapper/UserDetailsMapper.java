package com.salgosipo.global.security.account.mapper;

import com.salgosipo.security.account.domain.MemberVO;

public interface UserDetailsMapper {
    MemberVO get(String username);
}
