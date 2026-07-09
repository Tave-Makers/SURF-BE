package com.tavemakers.surf.presentation.post.controller.post;

import com.tavemakers.surf.application.post.usecase.PostDeleteUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.post.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물", description = "게시물 삭제 API")
public class PostDeleteController {

    private final PostDeleteUsecase postDeleteUsecase;

    /** 게시글 삭제 (작성자/권한 검증은 서비스에서) */
    @Operation(summary = "게시글 삭제", description = "본인이 작성한 게시글을 삭제합니다.")
    @DeleteMapping("/v1/user/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable(name = "postId") Long postId) {
        postDeleteUsecase.deletePost(postId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, POST_DELETED.getMessage());
    }
}
