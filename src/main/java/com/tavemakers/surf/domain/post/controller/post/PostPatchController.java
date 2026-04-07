package com.tavemakers.surf.domain.post.controller.post;

import com.tavemakers.surf.domain.post.dto.request.PostUpdateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.service.post.PostPatchUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.post.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물", description = "게시물 수정 API")
public class PostPatchController {

    private final PostPatchUsecase postPatchUsecase;

    /** 게시글 수정 (작성자 검증은 서비스에서) */
    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글을 수정합니다.")
    @PatchMapping("/v1/user/posts/{postId}")
    public ApiResponse<PostDetailResDTO> updatePost(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody PostUpdateReqDTO req
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        PostDetailResDTO response = postPatchUsecase.updatePost(postId, req, memberId);
        return ApiResponse.response(HttpStatus.OK, POST_UPDATED.getMessage(), response);
    }
}
