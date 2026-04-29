package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.response.BadgeDetailResDTO;
import com.tavemakers.surf.domain.badge.dto.response.BadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.usecase.BadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_LIST_READ;
import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_SINGLE_READ;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class BadgeGetController {

    private final BadgeUsecase badgeUsecase;

    /** 배지 목록 조회 */
    @Operation(summary = "배지 목록 조회", description = "활성화된 배지를 무한스크롤로 조회합니다.")
    @GetMapping("/v1/admin/badges")
    public ApiResponse<BadgeSliceResDTO> getBadgeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        BadgeSliceResDTO response =
                badgeUsecase.getBadgeList(size, page);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_LIST_READ.getMessage(),
                response
        );
    }

    /** 배지 단건 조회 */
    @Operation(summary = "배지 단건 조회", description = "특정 배지의 상세 정보를 조회합니다.")
    @GetMapping("/v1/admin/badges/{badgeId}")
    public ApiResponse<BadgeDetailResDTO> getBadge(
            @PathVariable Long badgeId
    ) {

        BadgeDetailResDTO response =
                badgeUsecase.getBadge(badgeId);

        return ApiResponse.response(
                HttpStatus.OK,
                BADGE_SINGLE_READ.getMessage(), // 필요하면 메시지 따로 분리
                response
        );
    }
}