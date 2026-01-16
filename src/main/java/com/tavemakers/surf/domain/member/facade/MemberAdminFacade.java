package com.tavemakers.surf.domain.member.facade;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.member.service.MemberPatchService;
import com.tavemakers.surf.domain.member.service.MemberService;
import com.tavemakers.surf.domain.score.service.PersonalScoreSaveService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberAdminFacade {

    private final MemberPatchService memberPatchService;
    private final MemberGetService memberGetService;
    private final MemberService memberService;
    private final PersonalScoreSaveService personalScoreSaveService;

    @Transactional
    public void changeRole (Long memberId, MemberRole role) {
        Member member = memberGetService.getMember(memberId);
        memberPatchService.grantRole(member, role);
    }

    @Transactional
    @LogEvent(value = "signup.approve", message = "회원가입 승인 처리")
    public void approveMember(
            @LogParam("member_id") Long memberId,
            @LogParam("approver_id") Long approverId
    ) {
        Member member = memberGetService.getMemberByStatus(memberId, MemberStatus.WAITING);
        memberService.approveMember(member);
        personalScoreSaveService.savePersonalScore(member);
    }

    @Transactional
    @LogEvent(value = "signup.reject", message = "회원가입 거절 처리")
    public void rejectMember(
            @LogParam("member_id") Long memberId,
            @LogParam("approver_id") Long approverId
    ) {
        Member member = memberGetService.getMemberByStatus(memberId, MemberStatus.WAITING);
        memberService.rejectMember(member);
    }
}
