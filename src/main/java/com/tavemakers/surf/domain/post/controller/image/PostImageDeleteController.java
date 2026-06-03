package com.tavemakers.surf.domain.post.controller.image;

import com.tavemakers.surf.domain.post.service.post.PostImageDeleteUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.post.controller.ResponseMessage.POST_IMAGE_DELETED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물", description = "게시물 이미지 삭제 API")
public class PostImageDeleteController {

    private final PostImageDeleteUsecase postImageDeleteUsecase;

    /** 게시글 이미지 삭제 (작성자/권한 검증은 서비스에서) */
    @Operation(summary = "게시글 이미지 삭제", description = "게시글에 첨부된 이미지를 삭제합니다. URL의 postId와 이미지가 속한 게시글이 일치해야 하며, S3 이미지는 트랜잭션 커밋 이후 삭제됩니다.")
    @DeleteMapping("/v1/user/posts/{postId}/images/{imageId}")
    public ApiResponse<Void> deletePostImage(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "imageId") Long imageId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        postImageDeleteUsecase.deletePostImage(postId, imageId, memberId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, POST_IMAGE_DELETED.getMessage());
    }
}
