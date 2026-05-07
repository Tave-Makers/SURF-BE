package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberGenerationSyncService {

    private final MemberRepository memberRepository;

    /** 현재 활동 기수 기준으로 승인 회원들의 활동 상태를 일괄 동기화합니다. */
    @Transactional
    public void syncApprovedMembersByGeneration(Integer activeGeneration) {
        List<Member> approvedMembers = memberRepository.findAllApprovedWithTracks();

        int ybCount = 0;
        int obCount = 0;
        int activeCount = 0;
        int inactiveCount = 0;

        for (Member member : approvedMembers) {
            MemberType memberType = resolveMemberType(member, activeGeneration);
            boolean isActive = hasTrackInGeneration(member, activeGeneration);
            member.syncGenerationStatus(memberType, isActive);

            if (memberType == MemberType.YB) {
                ybCount++;
            } else {
                obCount++;
            }

            if (isActive) {
                activeCount++;
            } else {
                inactiveCount++;
            }
        }

        log.info(
                "[ActiveGenerationSync] activeGeneration={}, approvedMembers={}, ybCount={}, obCount={}, activeCount={}, inactiveCount={}",
                activeGeneration,
                approvedMembers.size(),
                ybCount,
                obCount,
                activeCount,
                inactiveCount
        );
    }

    /** 단일 승인 회원을 현재 활동 기수 기준으로 동기화합니다. */
    @Transactional
    public void syncApprovedMember(Member member, Integer activeGeneration) {
        if (!member.isApproved() || member.isDeleted()) {
            return;
        }

        MemberType memberType = resolveMemberType(member, activeGeneration);
        boolean isActive = hasTrackInGeneration(member, activeGeneration);
        member.syncGenerationStatus(memberType, isActive);
    }

    private boolean hasTrackInGeneration(Member member, Integer activeGeneration) {
        return member.getTracks().stream()
                .map(Track::getGeneration)
                .anyMatch(activeGeneration::equals);
    }

    private MemberType resolveMemberType(Member member, Integer activeGeneration) {
        Integer firstGeneration = member.getFirstGeneration();
        return activeGeneration.equals(firstGeneration) ? MemberType.YB : MemberType.OB;
    }
}
