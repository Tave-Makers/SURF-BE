package com.tavemakers.surf.domain.post.controller;

import com.tavemakers.surf.domain.post.dto.res.PostLikeListResDTO;
import com.tavemakers.surf.domain.post.facade.PostFacade;
import com.tavemakers.surf.domain.post.service.PostLikeService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.post.controller.ResponseMessage.POST_LIKES_READ;
import static com.tavemakers.surf.domain.post.controller.ResponseMessage.POST_LIKE_CREATED;
import static com.tavemakers.surf.domain.post.controller.ResponseMessage.POST_LIKE_DELETED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물 좋아요", description = "게시물 좋아요 기능 관련 API")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final PostFacade postFacade;

    @Operation(summary = "좋아요 설정", description = "이미 좋아요 상태여도 200(OK) 반환")
    @PostMapping("/v1/user/posts/{postId}/like")
    public ApiResponse<Void> like(@PathVariable Long postId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        postLikeService.like(postId, memberId);
        return ApiResponse.response(HttpStatus.OK, POST_LIKE_CREATED.getMessage());
    }

    @Operation(summary = "좋아요 해제", description = "이미 좋아요 해제 상태여도 204(NO_CONTENT) 반환")
    @DeleteMapping("/v1/user/posts/{postId}/like")
    public ApiResponse<Void> unlike(@PathVariable Long postId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        postLikeService.unlike(postId, memberId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, POST_LIKE_DELETED.getMessage());
    }

    @Operation(summary = "특정 게시글 좋아요 리스트", description = "특정 게시글에 좋아요를 누른 유저의 리스트 반환")
    @GetMapping("/v1/user/posts/{postId}/like")
    public ApiResponse<PostLikeListResDTO> likePostList(@PathVariable Long postId) {
        return ApiResponse.response(HttpStatus.OK,POST_LIKES_READ.getMessage() ,postFacade.getPostLikes(postId));
    }
}
