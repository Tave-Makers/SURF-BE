package com.tavemakers.surf.domain.post.controller.post;

import com.tavemakers.surf.domain.post.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.domain.post.service.post.PostCreateUsecase;
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
@Tag(name = "게시물", description = "게시물 생성 API")
public class PostCreateController {

    private final PostCreateUsecase postCreateUsecase;

    /** 게시글 생성 (작성자 = 현재 로그인 사용자) */
    @Operation(summary = "게시글 생성", description = "게시글을 생성합니다.")
    @PostMapping("/v1/user/posts")
    public ApiResponse<PostDetailResDTO> createPost(
            @Valid @RequestBody PostCreateReqDTO req
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        PostDetailResDTO response = postCreateUsecase.createPost(req, memberId);
        return ApiResponse.response(HttpStatus.CREATED, POST_CREATED.getMessage(), response);
    }
}
