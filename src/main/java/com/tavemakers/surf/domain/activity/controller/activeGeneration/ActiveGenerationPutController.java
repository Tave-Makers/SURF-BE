package com.tavemakers.surf.domain.activity.controller.activeGeneration;

import com.tavemakers.surf.domain.activity.dto.acitveGeneration.request.ActiveGenerationUpdateReqDTO;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationPutService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.activity.controller.ResponseMessage.ACTIVE_GENERATION_UPDATED;

@RestController
@RequiredArgsConstructor
@Tag(name = "활동기수")
public class ActiveGenerationPutController {

    private final ActiveGenerationPutService activeGenerationPutService;

    @Operation(summary = "현재 활동 기수 변경")
    @PutMapping("/v1/admin/active-generation")
    public ApiResponse<Void> updateActiveGeneration(
            @RequestBody @Valid ActiveGenerationUpdateReqDTO dto) {
        activeGenerationPutService.updateActiveGeneration(dto.activeGeneration());
        return ApiResponse.response(HttpStatus.OK, ACTIVE_GENERATION_UPDATED.getMessage(), null);
    }
}
