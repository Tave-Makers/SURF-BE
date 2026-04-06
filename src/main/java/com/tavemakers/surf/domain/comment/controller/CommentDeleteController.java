package com.tavemakers.surf.domain.comment.controller;

import com.tavemakers.surf.domain.comment.usecase.CommentUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.comment.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "댓글", description = "댓글 및 대댓글 관련 CRUD API")
public class CommentDeleteController {

    private final CommentUsecase commentUsecase;

    @Operation(summary = "댓글 삭제 (내 댓글만)", description = "본인이 작성한 댓글만 삭제 가능, 댓글 및 대댓글 구분없이 hard 삭제 처리")
    @DeleteMapping("/v1/user/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long postId,
                                           @PathVariable Long commentId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        commentUsecase.deleteComment(postId, commentId, memberId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, COMMENT_DELETED.getMessage());
    }
}
