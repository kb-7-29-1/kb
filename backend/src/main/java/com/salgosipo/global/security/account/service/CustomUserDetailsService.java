package com.salgosipo.global.security.account.service;

import com.salgosipo.user.domain.UserVO;
import com.salgosipo.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.salgosipo.global.security.account.domain.CustomUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        UserVO vo = mapper.findByLoginId(loginId);
        if (vo == null) {
            throw new UsernameNotFoundException(loginId + "은 없는 id입니다.");
        }
        return new CustomUser(vo);
    }
}
