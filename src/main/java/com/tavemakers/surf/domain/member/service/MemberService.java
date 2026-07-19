package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.validator.OnboardingAccountValidator;
import com.tavemakers.surf.application.member.query.MemberBlacklistGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberBlacklistGetService memberBlacklistGetService;
    private final OnboardingAccountValidator onboardingAccountValidator;
    private final MemberRepository memberRepository;

    /**
     * 자체 회원가입 신청 완료 — 정규화·블랙리스트 검증·온보딩 계정 검증·가입 폼 반영. 트랙 추가와 DTO 매핑은 호출자(usecase)가 담당한다.
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
        // 이메일 필수 방어 — DTO 검증에 기대지 않는다. 빈 값이 findByEmail("") 조회·UNIQUE '' 점유로 샌다
        if (!StringUtils.hasText(rawEmail)) {
            throw new IllegalArgumentException("이메일은 필수 입력값입니다.");
        }

        // 이메일 및 전화번호 정규화 — 전화번호는 숫자만 남기고, 빈 결과는 null(미입력)로 접는다
        final String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        final String phoneDigits = rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
        final String normalizedPhone = phoneDigits.isEmpty() ? null : phoneDigits;

        memberBlacklistGetService.validateNotBlacklisted(null, normalizedEmail, normalizedPhone);

        // 온보딩 계정 검증 (case A/B/C, §3.5)
        onboardingAccountValidator.validateForOnboarding(member, normalizedEmail, normalizedPhone);

        // 회원가입 정보 반영
        member.applySignup(name, university, graduateSchool, normalizedEmail, normalizedPhone);

        // 경합 방어 — 커밋 전 email/phone unique 위반을 flush로 즉시 노출, 실제 값 충돌만 409로 변환 (§3.5.1)
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw translateOnboardingConflict(e, normalizedEmail, normalizedPhone);
        }

        return member;
    }

    /**
     * 무결성 위반이 email/phone 값 중복인지 root cause 메시지의 <b>중복 값</b>으로 판별한다
     * (제약 이름은 {@code @Column(unique=true)} 자동 생성이라 신뢰 불가). 그 외 위반은 원 예외를 그대로 전파한다.
     * 빈 문자열은 제외 — {@code String.contains("")}가 항상 true라 무관한 위반이 오분류된다.
     */
    private RuntimeException translateOnboardingConflict(
            DataIntegrityViolationException e,
            String normalizedEmail,
            String normalizedPhone
    ) {
        Throwable root = e.getMostSpecificCause();
        String message = root.getMessage() == null ? "" : root.getMessage().toLowerCase(Locale.ROOT);

        if (StringUtils.hasText(normalizedEmail) && message.contains(normalizedEmail)) {
            return new EmailAlreadyUsedException();
        }
        if (StringUtils.hasText(normalizedPhone) && message.contains(normalizedPhone)) {
            return new PhoneAlreadyUsedException();
        }
        return e;
    }

}
