package com.tavemakers.surf.presentation.block.controller;

import com.tavemakers.surf.application.block.usecase.BlockAdminUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.presentation.block.controller.ResponseMessage.ADMIN_BLOCK_RELEASED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "관리자 차단관리", description = "관리자용 차단 관계 관리 API")
public class BlockAdminDeleteController {

    private final BlockAdminUsecase blockAdminUsecase;

    /** 관리자 강제 해제 — 방향을 따지지 않고 block_id로 지운다 */
    @Operation(summary = "차단 강제 해제",
            description = "관리자가 차단 관계를 강제로 해제합니다. 사용자 해제와 달리 방향을 따지지 않으며, "
                    + "누가 무엇을 해제했는지 감사 로그가 남습니다.")
    @DeleteMapping("/v1/admin/blocks/{blockId}")
    public ApiResponse<Void> forceDeleteBlock(
            @Parameter(description = "차단 관계 ID", example = "101")
            @PathVariable Long blockId
    ) {
        Long adminId = SecurityUtils.getCurrentMemberId();
        blockAdminUsecase.forceDelete(adminId, blockId);
        return ApiResponse.response(HttpStatus.OK, ADMIN_BLOCK_RELEASED.getMessage());
    }
}
