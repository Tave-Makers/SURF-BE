package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.dto.response.MemberSignupResDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.domain.member.validator.OnboardingAccountValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberBlacklistGetService memberBlacklistGetService;
    private final OnboardingAccountValidator onboardingAccountValidator;
    private final MemberRepository memberRepository;

    /** 자체 회원가입 신청 완료 */
    @Transactional
    public MemberSignupResDTO signup(
            Member member,
            MemberSignupReqDTO request
    ) {
        // 이메일 및 전화번호 정규화
        final String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        final String normalizedPhone = request.getPhoneNumber() == null
                ? null
                : request.getPhoneNumber().replaceAll("\\D", "");

        memberBlacklistGetService.validateNotBlacklisted(null, normalizedEmail, normalizedPhone);

        // 통합 이메일·전화번호 기반 온보딩 검증 (case A 정상 / case B 통합 필요 감지 / case C 부분 일치 차단, §3.5)
        onboardingAccountValidator.validateForOnboarding(member, normalizedEmail, normalizedPhone);

        // 회원가입 정보 반영
        member.applySignup(request, normalizedEmail, normalizedPhone);

        // 동시성 경계: 사전 검증 통과 후에도 커밋 전 email/phone unique 위반이 발생할 수 있다(경합).
        // 여기서 즉시 flush 하여 위반을 동기적으로 노출하고, "실제 email/phone 충돌"일 때만 409(case C)로 변환한다.
        // 그 외 무결성 오류는 계정 충돌로 숨기지 않고 그대로 전파한다.
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw translateOnboardingConflict(e, normalizedEmail, normalizedPhone);
        }

        return MemberSignupResDTO.from(member);
    }

    /**
     * 온보딩 중 발생한 무결성 위반이 요청 email/phone 값의 중복인지 root cause 메시지로 판별한다.
     * 제약 이름은 {@code @Column(unique=true)} 자동 생성이라 신뢰할 수 없으므로, 이름이 아닌 <b>중복 값</b>으로 매칭한다
     * (MySQL: {@code Duplicate entry '값' for key ...}). email/phone 충돌이 아니면 원 예외를 그대로 반환해 일반 처리에 맡긴다.
     */
    private RuntimeException translateOnboardingConflict(
            DataIntegrityViolationException e,
            String normalizedEmail,
            String normalizedPhone
    ) {
        Throwable root = e.getMostSpecificCause();
        String message = root.getMessage() == null ? "" : root.getMessage().toLowerCase(Locale.ROOT);

        if (normalizedEmail != null && message.contains(normalizedEmail)) {
            return new EmailAlreadyUsedException();
        }
        if (normalizedPhone != null && message.contains(normalizedPhone)) {
            return new PhoneAlreadyUsedException();
        }
        return e;
    }

}
