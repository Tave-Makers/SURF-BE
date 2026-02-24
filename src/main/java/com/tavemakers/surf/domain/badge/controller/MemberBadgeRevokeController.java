package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.domain.badge.usecase.MemberBadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_REVOKED;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class MemberBadgeRevokeController {

    private final MemberBadgeUsecase memberBadgeUsecase;

    @Operation(summary = "배지 회수", description = "활동 배지를 선택된 회원들에게서 회수합니다.")
    @DeleteMapping("/v1/admin/badges/{badgeId}/members")
    public ApiResponse<Void> remove(
            @PathVariable Long badgeId,
            @RequestBody MemberBadgeReqDTO dto
    ) {

        memberBadgeUsecase.revoke(badgeId, dto);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_REVOKED.getMessage(),
                null
        );
    }
}