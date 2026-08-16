package com.tavemakers.surf.presentation.block.controller;

import com.tavemakers.surf.application.block.usecase.BlockAdminUsecase;
import com.tavemakers.surf.domain.block.entity.enums.BlockDirection;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.presentation.block.dto.response.BlockAdminSliceResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.block.controller.ResponseMessage.ADMIN_BLOCK_LIST_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "관리자 차단관리", description = "관리자용 차단 관계 관리 API")
public class BlockAdminGetController {

    private final BlockAdminUsecase blockAdminUsecase;

    /** 차단 관계 목록 — 전체 또는 특정 회원 기준 */
    @Operation(summary = "차단 관계 목록",
            description = "전체 차단 관계를 최신순으로 조회합니다. memberId를 주면 해당 회원 기준으로 "
                    + "필터링하며, direction으로 방향을 지정할 수 있습니다(생략 시 ALL).")
    @GetMapping("/v1/admin/blocks")
    public ApiResponse<BlockAdminSliceResDTO> getBlocks(
            @Parameter(description = "기준 회원 ID (없으면 전체 조회)", example = "12")
            @RequestParam(required = false) Long memberId,

            @Parameter(description = "조회 방향 — memberId가 있을 때만 의미가 있습니다", example = "ALL")
            @RequestParam(required = false) BlockDirection direction,

            @PageableDefault(size = 20) Pageable pageable
    ) {
        BlockAdminSliceResDTO response = blockAdminUsecase.getBlocks(memberId, direction, pageable);
        return ApiResponse.response(HttpStatus.OK, ADMIN_BLOCK_LIST_READ.getMessage(), response);
    }
}
