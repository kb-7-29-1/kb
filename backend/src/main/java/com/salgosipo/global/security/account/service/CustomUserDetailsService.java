package com.salgosipo.global.security.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.salgosipo.security.account.domain.CustomUser;
import com.salgosipo.security.account.domain.MemberVO;
import com.salgosipo.security.account.mapper.UserDetailsMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDetailsMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberVO vo = mapper.get(username);
        if (vo == null) {
            throw new UsernameNotFoundException(username + "은 없는 id입니다.");
        }
        return new CustomUser(vo);
    }
}
