package com.tavemakers.surf.domain.comment.presentation.controller;

import com.tavemakers.surf.domain.comment.domain.service.CommentLikeService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tavemakers.surf.domain.comment.presentation.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "댓글 좋아요", description = "댓글 좋아요 / 취소 / 상태 관련 API")
public class CommentLikeCreateController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "댓글 좋아요 토글", description = "이미 누른 경우 취소, 안 누른 경우 좋아요로 변경")
    @PostMapping("/v1/user/comments/{commentId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long commentId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        boolean liked = commentLikeService.toggleLike(commentId, memberId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_LIKE_TOGGLED.getMessage(), Map.of("liked", liked));
    }
}
