package com.tavemakers.surf.domain.comment.controller;

import com.tavemakers.surf.domain.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.domain.comment.usecase.CommentUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.comment.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "댓글", description = "댓글 및 대댓글 관련 CRUD API")
public class CommentCreateController {

    private final CommentUsecase commentUsecase;

    /** 댓글 생성 (루트 댓글 또는 대댓글) */
    @Operation(summary = "댓글 생성 (루트/대댓글)", description = "rootId가 null이면 루트 댓글")
    @PostMapping("/v1/user/posts/{postId}/comments")
    public ApiResponse<CommentResDTO> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateReqDTO req) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        CommentResDTO response = commentUsecase.createComment(postId, memberId, req);
        return ApiResponse.response(HttpStatus.CREATED, COMMENT_CREATED.getMessage(), response);
    }
}
