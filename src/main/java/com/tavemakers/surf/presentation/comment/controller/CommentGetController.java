package com.tavemakers.surf.presentation.comment.controller;

import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.application.comment.usecase.CommentUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.comment.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "댓글", description = "댓글 및 대댓글 관련 CRUD API")
public class CommentGetController {

    private final CommentUsecase commentUsecase;

    @Operation(summary = "댓글 목록 조회 (페이징)", description = "루트 댓글과 대댓글 모두 포함. 페이징 처리")
    @GetMapping("/v1/user/posts/{postId}/comments")
    public ApiResponse<CommentListResDTO> getComments(
            @PathVariable Long postId,
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ){
        Long memberId = SecurityUtils.getCurrentMemberId();
        CommentListResDTO data = commentUsecase.getComments(postId, pageable, memberId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_READ.getMessage(), data);
    }

    /** 댓글 단건 조회 */
    @Operation(summary = "댓글 단건 조회", description = "댓글 ID로 특정 댓글 하나를 조회합니다. 신고 화면처럼 대상 댓글만 필요할 때 사용합니다.")
    @GetMapping("/v1/user/comments/{commentId}")
    public ApiResponse<CommentResDTO> getComment(@PathVariable Long commentId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        CommentResDTO data = commentUsecase.getComment(commentId, memberId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_READ.getMessage(), data);
    }
}
