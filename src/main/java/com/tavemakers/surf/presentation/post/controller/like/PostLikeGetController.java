package com.tavemakers.surf.presentation.post.controller.like;

import com.tavemakers.surf.presentation.post.dto.response.PostLikeListResDTO;
import com.tavemakers.surf.application.post.usecase.PostUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.post.controller.ResponseMessage.POST_LIKES_READ;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물 좋아요", description = "게시물 좋아요 기능 관련 API")
public class PostLikeGetController {

    private final PostUsecase postUsecase;

    @Operation(summary = "특정 게시글 좋아요 리스트", description = "특정 게시글에 좋아요를 누른 유저의 리스트 반환")
    @GetMapping("/v1/user/posts/{postId}/like")
    public ApiResponse<PostLikeListResDTO> likePostList(@PathVariable Long postId) {
        return ApiResponse.response(HttpStatus.OK, POST_LIKES_READ.getMessage(), postUsecase.getPostLikes(postId));
    }
}
