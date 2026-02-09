package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.dto.response.MemberSignupResDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

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

        // 회원가입 정보 반영
        member.applySignup(request, normalizedEmail, normalizedPhone);

        return MemberSignupResDTO.from(member);
    }

}
