package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.usecase.BadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_DELETED;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class BadgeDeleteController {

    private final BadgeUsecase badgeUsecase;

    @Operation(summary = "배지 삭제", description = "배지를 삭제합니다.")
    @DeleteMapping("/v1/admin/badges/{badgeId}")
    public ApiResponse<Void> delete(@PathVariable Long badgeId) {

        badgeUsecase.delete(badgeId);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_DELETED.getMessage(),
                null
        );
    }
}