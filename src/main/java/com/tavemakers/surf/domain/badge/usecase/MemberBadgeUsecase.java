package com.tavemakers.surf.domain.badge.usecase;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeResDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberOwnedBadgeResDTO;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.service.*;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberBadgeUsecase {

    private final MemberBadgeAssignService memberBadgeAssignService;
    private final MemberBadgeRevokeService memberBadgeRevokeService;
    private final MemberBadgeGetService memberBadgeGetService;
    private final BadgeGetService badgeGetService;
    private final LogEventEmitter logEventEmitter;

    /** 배지 부여 */
    @Transactional
    public void assignBadge(Long badgeId, MemberBadgeReqDTO dto) {

        memberBadgeAssignService.assignBadge(badgeId, dto.getMemberIds());
        Badge badge = badgeGetService.getBadgeDetail(badgeId);

        logEventEmitter.emit("badge.granted", Map.of(
                "badge_id", badgeId,
                "badge_name", badge.getName(),
                "member_ids", dto.getMemberIds(),
                "awarded_by", SecurityUtils.getCurrentMemberId()
        ));
    }

    /** 특정 배지 받은 회원 목록 조회 */
    @Transactional(readOnly = true)
    public MemberBadgeSliceResDTO getMembersByBadge(
            Long badgeId,
            int pageSize,
            int pageNum
    ) {

        // 페이지 번호, 사이즈, 정렬조건(최신순) 기반 Pageable 생성
        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by(Sort.Direction.DESC, "awardedAt")
        );

        // 배지 기준 회원 목록 Slice 조회
        Slice<MemberBadge> slice =
                memberBadgeGetService.getMembersByBadge(badgeId, pageable);

        return MemberBadgeSliceResDTO.from(slice.map(MemberBadgeResDTO::from));
    }

    /** 배지 회수 */
    @Transactional
    public void revokeBadge(Long badgeId, MemberBadgeReqDTO dto) {
        memberBadgeRevokeService.revokeBadge(badgeId, dto.getMemberIds());
    }

    /** 특정 회원의 배지 전체 조회 */
    @Transactional(readOnly = true)
    public List<MemberOwnedBadgeResDTO> getAllMemberBadges(Long memberId) {
        return memberBadgeGetService.getAllByMemberId(memberId);
    }
}