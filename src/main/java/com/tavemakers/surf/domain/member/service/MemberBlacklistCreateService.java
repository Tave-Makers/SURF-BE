package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.MemberBlacklist;
import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.domain.member.repository.MemberBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MemberBlacklistCreateService {

    private final MemberBlacklistRepository memberBlacklistRepository;

    @Transactional
    public void createIfAbsent(Member member, MemberBlacklistActionType actionType, Long processedBy) {
        Long kakaoId = member.getKakaoId();
        String normalizedEmail = normalizeEmail(member.getEmail());
        String normalizedPhoneNumber = normalizePhoneNumber(member.getPhoneNumber());

        if (normalizedEmail == null) {
            throw new IllegalStateException("블랙리스트 생성 실패: 회원 이메일 없음");
        }

        if (isBlacklisted(kakaoId, normalizedEmail, normalizedPhoneNumber)) {
            return;
        }

        memberBlacklistRepository.save(
                MemberBlacklist.of(member, actionType, processedBy, normalizedEmail, normalizedPhoneNumber)
        );
    }

    private boolean isBlacklisted(Long kakaoId, String email, String phoneNumber) {
        return (kakaoId != null && memberBlacklistRepository.existsByKakaoId(kakaoId))
                || (StringUtils.hasText(email) && memberBlacklistRepository.existsByEmail(email))
                || (StringUtils.hasText(phoneNumber) && memberBlacklistRepository.existsByPhoneNumber(phoneNumber));
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
