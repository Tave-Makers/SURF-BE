package com.tavemakers.surf.domain.post.presentation.controller.like;

import com.tavemakers.surf.domain.post.domain.service.like.PostLikeService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.post.presentation.controller.ResponseMessage.POST_LIKE_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물 좋아요", description = "게시물 좋아요 기능 관련 API")
public class PostLikeCreateController {

    private final PostLikeService postLikeService;

    @Operation(summary = "좋아요 설정", description = "이미 좋아요 상태여도 200(OK) 반환")
    @PostMapping("/v1/user/posts/{postId}/like")
    public ApiResponse<Void> like(@PathVariable Long postId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        postLikeService.like(postId, memberId);
        return ApiResponse.response(HttpStatus.OK, POST_LIKE_CREATED.getMessage());
    }
}
