package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.dto.response.HomeContentResDTO;
import com.tavemakers.surf.domain.home.usecase.HomeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.HOME_CONTENT_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "홈 문구 관리", description = "홈 문구 관리 API")
public class HomeContentGetController {

    private final HomeUsecase homeUsecase;

    @Operation(summary = "홈 문구 조회", description = "홈 문구를 조회합니다.")
    @GetMapping("/v1/admin/home/content")
    public ApiResponse<HomeContentResDTO> getContent() {
        HomeContentResDTO response = homeUsecase.getContent();
        return ApiResponse.response(HttpStatus.OK, HOME_CONTENT_READ.getMessage(), response);
    }
}
