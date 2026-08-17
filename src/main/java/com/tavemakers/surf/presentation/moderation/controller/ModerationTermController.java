package com.tavemakers.surf.presentation.moderation.controller;

import com.tavemakers.surf.application.moderation.query.ModerationTermGetService;
import com.tavemakers.surf.application.moderation.usecase.ModerationTermUsecase;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.presentation.moderation.dto.request.ModerationTermCreateReqDTO;
import com.tavemakers.surf.presentation.moderation.dto.response.ModerationTermResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tavemakers.surf.presentation.moderation.controller.ResponseMessage.MODERATION_TERM_CREATED;
import static com.tavemakers.surf.presentation.moderation.controller.ResponseMessage.MODERATION_TERM_DELETED;
import static com.tavemakers.surf.presentation.moderation.controller.ResponseMessage.MODERATION_TERM_READ;

/**
 * 금칙어 사전 관리 API — 관리자급(ADMIN·MANAGER·PRESIDENT) 전용.
 *
 * <p>사전 편집은 오탐 대응의 1차 수단이므로 운영을 담당하는 관리자급 역할에 열어 둔다.
 * 클래스 단위 @PreAuthorize 로 해당 역할만 통과시킨다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/moderation/terms")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRESIDENT')")
@Tag(name = "금칙어 사전", description = "금칙어·허용 표현 사전 관리 API (관리자급 ADMIN·MANAGER·PRESIDENT 전용)")
public class ModerationTermController {

    private final ModerationTermUsecase moderationTermUsecase;
    private final ModerationTermGetService moderationTermGetService;

    /** 사전 항목 등록 */
    @Operation(summary = "금칙어 사전 항목 등록",
            description = "금칙어(BANNED) 또는 허용 표현(ALLOWED)을 등록합니다. 커밋 이후 마스킹 엔진에 반영됩니다.")
    @PostMapping
    public ApiResponse<ModerationTermResDTO> createTerm(
            @Valid @RequestBody ModerationTermCreateReqDTO dto
    ) {
        ModerationTermResDTO response = moderationTermUsecase.createTerm(dto);
        return ApiResponse.response(HttpStatus.CREATED, MODERATION_TERM_CREATED.getMessage(), response);
    }

    /** 사전 항목 목록 조회 */
    @Operation(summary = "금칙어 사전 항목 목록 조회",
            description = "사전 항목을 조회합니다. type 을 지정하면 해당 종류만 반환합니다.")
    @GetMapping
    public ApiResponse<List<ModerationTermResDTO>> getTerms(
            @RequestParam(required = false) ModerationTermType type
    ) {
        List<ModerationTermResDTO> response = moderationTermGetService.getTerms(type);
        return ApiResponse.response(HttpStatus.OK, MODERATION_TERM_READ.getMessage(), response);
    }

    /** 사전 항목 삭제 */
    @Operation(summary = "금칙어 사전 항목 삭제", description = "사전 항목을 삭제합니다.")
    @DeleteMapping("/{termId}")
    public ApiResponse<Void> deleteTerm(@PathVariable Long termId) {
        moderationTermUsecase.deleteTerm(termId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, MODERATION_TERM_DELETED.getMessage(), null);
    }

}
