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

    /** 통합 이메일·전화번호를 기준으로 블랙리스트 여부를 검증한다(provider 이메일은 식별에 쓰지 않음, 5.A-8). */
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
