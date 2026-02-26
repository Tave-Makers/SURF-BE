package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.request.BadgeUpdateReqDTO;
import com.tavemakers.surf.domain.badge.usecase.BadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_UPDATED;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class BadgeUpdateController {

    private final BadgeUsecase badgeUsecase;

    @Operation(summary = "배지 수정", description = "기존 배지 정보를 수정합니다.")
    @PatchMapping("/v1/admin/badges/{badgeId}")
    public ApiResponse<Void> update(
            @PathVariable Long badgeId,
            @Valid @RequestBody BadgeUpdateReqDTO dto
    ) {

        badgeUsecase.update(badgeId, dto);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_UPDATED.getMessage(),
                null
        );
    }
}