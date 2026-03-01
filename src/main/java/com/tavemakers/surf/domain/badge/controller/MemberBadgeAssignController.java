package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.usecase.MemberBadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_ASSIGNED;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class MemberBadgeAssignController {

    private final MemberBadgeUsecase memberBadgeUsecase;

    /** 회원들을 선택히여 활동 배지 부여 */
    @Operation(summary = "배지 부여", description = "활동 배지를 선택된 회원들에게 부여합니다.")
    @PostMapping("/v1/admin/badges/{badgeId}/members")
    public ApiResponse<Void> assign(
            @PathVariable Long badgeId,
            @Valid @RequestBody MemberBadgeReqDTO dto
    ) {

        memberBadgeUsecase.assign(badgeId, dto);

        return ApiResponse.response(
                HttpStatus.CREATED,
                BADGE_ASSIGNED.getMessage(),
                null
        );
    }
}