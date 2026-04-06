package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.dto.response.HomeResDTO;
import com.tavemakers.surf.domain.home.usecase.HomeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.HOME_PAGE_RENDERED;

@RestController
@RequiredArgsConstructor
@Tag(name = "홈 화면 렌더링")
public class HomeController {

    private final HomeUsecase homeUsecase;

    @Operation(summary = "홈 화면 렌더링", description = "홈 화면에 필요한 데이터를 렌더링합니다.")
    @GetMapping("/v1/user/home")
    public ApiResponse<HomeResDTO> home() {
        HomeResDTO response = homeUsecase.getHome();
        return ApiResponse.response(HttpStatus.OK, HOME_PAGE_RENDERED.getMessage(), response);
    }
}