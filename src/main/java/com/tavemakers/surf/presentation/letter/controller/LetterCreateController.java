package com.tavemakers.surf.presentation.letter.controller;

import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
import com.tavemakers.surf.application.letter.usecase.LetterUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.letter.controller.ResponseMessage.LETTER_CREATED;

@RestController
@RequiredArgsConstructor
@Tag(name = "쪽지", description = "회원 간 쪽지 전송 및 조회에 대한 API 입니다.")
public class LetterCreateController {

    private final LetterUsecase letterUsecase;

    @PostMapping("/v1/user/letters")
    @Operation(summary = "쪽지 전송", description = "로그인한 사용자가 다른 회원에게 쪽지를 전송합니다.")
    public ApiResponse<LetterResDTO> createLetter(
            @Valid @RequestBody LetterCreateReqDTO request
    ) {
        Long senderId = SecurityUtils.getCurrentMemberId();

        return ApiResponse.response(
                HttpStatus.OK,
                LETTER_CREATED.getMessage(),
                letterUsecase.createLetter(senderId, request)
        );
    }
}
