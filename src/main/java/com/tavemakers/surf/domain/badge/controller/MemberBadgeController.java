package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.facade.MemberBadgeFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동뱃지")
public class MemberBadgeController {

    private final MemberBadgeFacade memberBadgeFacade;

    @Operation(
            summary = "활동 뱃지 부여 API",
            description = "활동 뱃지 부여 API"
    )
    @PostMapping("/v1/admin/members/badges")
    public ApiResponse<Void> createBadge(@RequestBody @Valid MemberBadgeReqDTO dto) {
        memberBadgeFacade.saveMemberBadgeList(dto);
        return ApiResponse.response(HttpStatus.CREATED, MEMBER_BADGE_LIST_CREATED.getMessage(), null);
    }

    @Operation(
            summary = "마이페이지 [활동 뱃지] 조회",
            description = "마이페이지 [활동 뱃지] 조회"
    )
    @GetMapping("/v1/user/members/badges")
    public ApiResponse<MemberBadgeSliceResDTO> getMyBadges(
            @RequestParam(required = false) Long memberId,
            @RequestParam int pageSize,
            @RequestParam int pageNum
    ) {
        memberId = (memberId == null ? SecurityUtils.getCurrentMemberId() : memberId);
        MemberBadgeSliceResDTO response = memberBadgeFacade.getMemberBadgeWithSlice(memberId, pageSize, pageNum);
        return ApiResponse.response(HttpStatus.OK, MEMBER_BADGE_LIST_READ.getMessage(), response);
    }

}
