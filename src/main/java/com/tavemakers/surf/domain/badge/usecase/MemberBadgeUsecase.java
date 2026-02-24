package com.tavemakers.surf.domain.badge.usecase;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeResDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberOwnedBadgeResDTO;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberBadgeUsecase {

    private final MemberBadgeGrantService memberBadgeCreateService;
    private final MemberBadgeDeleteService memberBadgeDeleteService;
    private final MemberBadgeGetService memberBadgeGetService;

    /** 배지 부여 */
    public void assign(Long badgeId, MemberBadgeReqDTO dto) {
        memberBadgeCreateService.assign(badgeId, dto.getMemberIds());
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
    public void revoke(Long badgeId, MemberBadgeReqDTO dto) {
        memberBadgeDeleteService.revoke(badgeId, dto.getMemberIds());
    }

    /** 특정 회원의 배지 전체 조회 */
    @Transactional(readOnly = true)
    public List<MemberOwnedBadgeResDTO> getAllMemberBadges(Long memberId) {
        return memberBadgeGetService.getAllByMemberId(memberId);
    }
}