package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * DB에서 회원 정보를 조회해 UserDetails(CustomUserDetails)로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.APPROVED)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> {
                    log.warn("Login blocked for email(masked): {}", maskEmail(email));
                    return new UsernameNotFoundException("사용자 정보를 확인할 수 없습니다.");
                });
    }

    private static String maskEmail(String email) {
        return email == null ? null : email.replaceAll("(?<=.).(?=[^@]*?@)", "*");
    }
}
