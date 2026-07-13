package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberBlacklistGetService memberBlacklistGetService;

    /**
     * 자체 회원가입 신청 완료 — 정규화·블랙리스트 검증·가입 폼 반영. 트랙 추가와 DTO 매핑은 호출자(usecase)가 담당한다.
     * 트랜잭션 경계는 호출자(usecase)가 소유한다.
     */
    public Member signup(
            Member member,
            String name,
            String university,
            String graduateSchool,
            String rawEmail,
            String rawPhone
    ) {
        // 이메일 및 전화번호 정규화
        final String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        final String normalizedPhone = rawPhone == null
                ? null
                : rawPhone.replaceAll("\\D", "");

        memberBlacklistGetService.validateNotBlacklisted(null, normalizedEmail, normalizedPhone);

        // 회원가입 정보 반영
        member.applySignup(name, university, graduateSchool, normalizedEmail, normalizedPhone);

        return member;
    }

}
