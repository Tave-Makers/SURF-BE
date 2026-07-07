package com.tavemakers.surf.domain.member.domain.service;

import com.tavemakers.surf.domain.member.presentation.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.presentation.dto.response.MemberSignupResDTO;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.application.query.MemberBlacklistGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberBlacklistGetService memberBlacklistGetService;

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

        // 회원가입 정보 반영
        member.applySignup(request, normalizedEmail, normalizedPhone);

        return MemberSignupResDTO.from(member);
    }

}
