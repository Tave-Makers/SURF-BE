package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.dto.request.HomeBannerCreateReqDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.domain.home.usecase.HomeUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "홈 배너 관리", description = "홈 배너 관리 API")
public class HomeBannerCreateController {

    private final HomeUsecase homeUsecase;

    @Operation(summary = "홈 배너 생성", description = "새로운 홈 배너를 생성합니다.")
    @PostMapping("/v1/admin/home/banners")
    public ApiResponse<HomeBannerResDTO> createBanner(
            @RequestBody @Valid HomeBannerCreateReqDTO req
    ) {
        HomeBannerResDTO response = homeUsecase.createBanner(req);
        return ApiResponse.response(HttpStatus.CREATED, HOME_BANNER_CREATED.getMessage(), response);
    }
}
