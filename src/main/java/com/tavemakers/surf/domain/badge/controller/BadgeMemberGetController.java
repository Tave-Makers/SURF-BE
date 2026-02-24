package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.response.MemberBadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.usecase.MemberBadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_MEMBER_LIST_READ;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class BadgeMemberGetController {

    private final MemberBadgeUsecase memberBadgeUsecase;

    @Operation(summary = "해당 배지 받은 회원 조회", description = "해당 배지를 받은 모든 회원의 목록을 조회합니다.")
    @GetMapping("/v1/admin/badges/{badgeId}/members")
    public ApiResponse<MemberBadgeSliceResDTO> getMembers(
            @PathVariable Long badgeId,
            @Parameter(example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {

        MemberBadgeSliceResDTO response =
                memberBadgeUsecase.getMembersByBadge(badgeId, size, page);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_MEMBER_LIST_READ.getMessage(),
                response
        );
    }
}