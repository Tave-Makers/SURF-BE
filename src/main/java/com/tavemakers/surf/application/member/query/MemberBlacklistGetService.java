package com.tavemakers.surf.application.member.query;

import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.repository.MemberBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBlacklistGetService {

    private final MemberBlacklistRepository memberBlacklistRepository;

    /**
     * 블랙리스트 검증 — 기준은 통합 이메일/전화번호(Member.email / Member.phoneNumber)다 (5.A-8).
     * provider가 준 이메일(SocialAccount.providerEmail)은 회원 식별에 쓰지 않으므로 검증 입력으로 삼지 않는다.
     */
    public void validateNotBlacklisted(String email, String phoneNumber) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

        boolean blacklisted = (StringUtils.hasText(normalizedEmail) && memberBlacklistRepository.existsByEmail(normalizedEmail))
                || (StringUtils.hasText(normalizedPhoneNumber) && memberBlacklistRepository.existsByPhoneNumber(normalizedPhoneNumber));

        if (blacklisted) {
            throw new MemberBlacklistedException();
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return null;
        }
        return phoneNumber.replaceAll("\\D", "");
    }
}
