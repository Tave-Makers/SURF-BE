package com.tavemakers.surf.domain.member.service;

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

    public void validateNotBlacklisted(Long kakaoId, String email, String phoneNumber) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);

        boolean blacklisted = (kakaoId != null && memberBlacklistRepository.existsByKakaoId(kakaoId))
                || (StringUtils.hasText(normalizedEmail) && memberBlacklistRepository.existsByEmail(normalizedEmail))
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
