package com.tavemakers.surf.presentation.letter.controller;

import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
import com.tavemakers.surf.application.letter.usecase.LetterUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.letter.controller.ResponseMessage.LETTER_SENT_READ;

@RestController
@RequiredArgsConstructor
@Tag(name = "쪽지", description = "회원 간 쪽지 전송 및 조회에 대한 API 입니다.")
public class LetterGetController {

    private final LetterUsecase letterUsecase;

    @GetMapping("/v1/user/letters/sent")
    @Operation(summary = "쪽지 조회", description = "자신이 보낸 쪽지 목록을 확인합니다.")
    public ApiResponse<Slice<LetterResDTO>> getSentLetters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long senderId = SecurityUtils.getCurrentMemberId();

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ApiResponse.response(
                HttpStatus.OK,
                LETTER_SENT_READ.getMessage(),
                letterUsecase.getSentLetters(senderId, pageable)
        );
    }
}
