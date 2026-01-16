package com.tavemakers.surf.domain.badge.facade;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeResDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.service.MemberBadgeGetService;
import com.tavemakers.surf.domain.badge.service.MemberBadgeSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberBadgeFacade {

    private final MemberBadgeSaveService memberBadgeSaveService;
    private final MemberBadgeGetService memberBadgeGetService;

    public void saveMemberBadgeList(MemberBadgeReqDTO dto) {
        memberBadgeSaveService.saveMemberBadgeList(dto);
    }

    public MemberBadgeSliceResDTO getMemberBadgeWithSlice(Long memberId, int pageSize, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "awardedAt"));
        Slice<MemberBadge> slice
                = memberBadgeGetService.findMemberBadgeWithSlice(memberId, pageable);
        return MemberBadgeSliceResDTO.from(slice.map(MemberBadgeResDTO::from));
    }

}
