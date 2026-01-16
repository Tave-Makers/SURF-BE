package com.tavemakers.surf.domain.home.controller;

import com.tavemakers.surf.domain.home.dto.request.HomeContentUpsertReqDTO;
import com.tavemakers.surf.domain.home.dto.response.HomeContentResDTO;
import com.tavemakers.surf.domain.home.facade.HomeFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.home.controller.ResponseMessage.HOME_CONTENT_READ;
import static com.tavemakers.surf.domain.home.controller.ResponseMessage.HOME_CONTENT_UPSERTED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "홈 문구 관리", description = "홈 문구 관리 API")
public class HomeContentAdminController {

    private final HomeFacade homeFacade;

    @Operation(summary = "홈 문구 조회", description = "홈 문구를 조회합니다.")
    @GetMapping("/v1/admin/home/content")
    public ApiResponse<HomeContentResDTO> getContent() {
        HomeContentResDTO response = homeFacade.getContent();
        return ApiResponse.response(HttpStatus.OK, HOME_CONTENT_READ.getMessage(), response);
    }

    @Operation(summary = "홈 문구 생성/수정", description = "홈 문구를 생성하거나 수정합니다.")
    @PutMapping("/v1/admin/home/content")
    public ApiResponse<HomeContentResDTO> upsertContent(
            @RequestBody @Valid HomeContentUpsertReqDTO req
    ) {
        HomeContentResDTO response = homeFacade.upsertContent(req);
        return ApiResponse.response(HttpStatus.OK, HOME_CONTENT_UPSERTED.getMessage(), response);
    }
}