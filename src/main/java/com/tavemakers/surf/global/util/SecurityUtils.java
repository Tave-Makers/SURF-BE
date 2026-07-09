package com.tavemakers.surf.global.util;

import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("현재 인증된 사용자가 없습니다.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new IllegalStateException("인증 주체가 CustomUserDetails가 아닙니다.");
        }
        return (CustomUserDetails) principal;
    }

    public static Member getCurrentMember() {
        return getCurrentUserDetails().getMember();
    }

    public static Long getCurrentMemberId() {
        return getCurrentMember().getId();
    }

    public static Long getCurrentKakaoId(){return getCurrentMember().getKakaoId();}

    public static String getCurrentMemberRole(){return getCurrentMember().getRole().toString();}
}