package com.tavemakers.surf.domain.badge.controller;

import com.tavemakers.surf.domain.badge.dto.request.BadgeCreateReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.BadgeCreateResDTO;
import com.tavemakers.surf.domain.badge.usecase.BadgeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import static com.tavemakers.surf.domain.badge.controller.ResponseMessage.BADGE_CREATED;

@RestController
@RequiredArgsConstructor
@Tag(name = "배지", description = "활동 배지 관리 및 조회 관련 API")
public class BadgeCreateController {

    private final BadgeUsecase badgeUsecase;

    /** 새로운 활동 배지 생성 */
    @Operation(summary = "배지 생성", description = "새로운 활동 배지를 생성합니다.")
    @PostMapping("/v1/admin/badges")
    public ApiResponse<BadgeCreateResDTO> createBadge(@Valid @RequestBody BadgeCreateReqDTO dto) {

        Long badgeId = badgeUsecase.createBadge(dto);

        return ApiResponse.response(
                HttpStatus.CREATED,
                BADGE_CREATED.getMessage(),
                BadgeCreateResDTO.of(badgeId)
        );
    }
}