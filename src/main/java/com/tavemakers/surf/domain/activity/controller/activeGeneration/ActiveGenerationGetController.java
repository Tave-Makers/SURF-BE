package com.tavemakers.surf.domain.activity.controller.activeGeneration;

import com.tavemakers.surf.domain.activity.dto.acitveGeneration.response.ActiveGenerationMemberResDTO;
import com.tavemakers.surf.domain.activity.dto.acitveGeneration.response.ActiveGenerationResDTO;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationGetService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.ACTIVE_GENERATION_READ;
import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.MEMBER_OF_ACTIVE_GENERATION_READ;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기수")
public class ActiveGenerationGetController {

    private final ActiveGenerationGetService activeGenerationGetService;

    @Operation(summary = "현재 활동 기수 조회")
    @GetMapping("/v1/manager/active-generation")
    public ApiResponse<ActiveGenerationResDTO> getActiveGeneration() {
        Integer generation = activeGenerationGetService.getActiveGeneration();
        ActiveGenerationResDTO response = ActiveGenerationResDTO.of(generation);
        return ApiResponse.response(HttpStatus.OK, ACTIVE_GENERATION_READ.getMessage(), response);
    }

    @GetMapping("/v1/manager/active-generation/members")
    public ApiResponse<List<ActiveGenerationMemberResDTO>> getActiveGenerationMembers() {
        List<ActiveGenerationMemberResDTO> response = activeGenerationGetService.getActiveGenerationMembers();
        return ApiResponse.response(HttpStatus.OK, MEMBER_OF_ACTIVE_GENERATION_READ.getMessage(), response);
    }
}