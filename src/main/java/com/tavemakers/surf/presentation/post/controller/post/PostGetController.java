package com.tavemakers.surf.presentation.post.controller.post;

import com.tavemakers.surf.presentation.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.post.service.post.PostListService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.post.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물", description = "게시물 조회 API")
public class PostGetController {

    private final PostGetService postGetService;
    private final PostListService postListService;

    /** 게시글 단건 조회 (뷰어 = 현재 로그인 사용자; 스크랩 여부 등 계산용) */
    @Operation(summary = "게시글 단건 조회", description = "특정 ID의 게시글을 조회합니다.")
    @GetMapping("/v1/user/posts/{postId}")
    public ApiResponse<PostDetailResDTO> getPost(
            @PathVariable(name = "postId") Long postId
    ) {
        Long viewerId = SecurityUtils.getCurrentMemberId();
        PostDetailResDTO response = postGetService.getPostDetail(postId, viewerId);
        return ApiResponse.response(HttpStatus.OK, POST_READ.getMessage(), response);
    }

    /** 내가 작성한 게시글 목록 */
    @Operation(summary = "내가 작성한 게시글", description = "본인이 작성한 게시글 목록을 조회합니다.")
    @GetMapping("/v1/user/posts/me")
    public ApiResponse<Slice<PostResDTO>> getMyPosts(
            @PageableDefault(size = 12, sort = "postedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long me = SecurityUtils.getCurrentMemberId();
        Slice<PostResDTO> response = postListService.getMyPosts(me, pageable);
        return ApiResponse.response(HttpStatus.OK, MY_POSTS_READ.getMessage(), response);
    }

    /** 특정 작성자의 게시글 목록 (뷰어 = 현재 로그인 사용자) */
    @Operation(summary = "특정 작성자의 게시글 목록", description = "특정 작성자가 작성한 게시글 목록을 조회합니다.")
    @GetMapping("/v1/user/posts/author/{authorId}")
    public ApiResponse<Slice<PostResDTO>> getPostsByMember(
            @PathVariable Long authorId,
            @PageableDefault(size = 12, sort = "postedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long viewerId = SecurityUtils.getCurrentMemberId();
        Slice<PostResDTO> response = postListService.getPostsByMember(authorId, viewerId, pageable);
        return ApiResponse.response(HttpStatus.OK, POSTS_BY_MEMBER_READ.getMessage(), response);
    }

    @Operation(summary = "보드 게시글 목록", description = "category 미지정 또는 'all'이면 보드 전체")
    @GetMapping("/v1/user/posts/board/{boardId}")
    public ApiResponse<Slice<PostResDTO>> getPostsByBoardAndCategory(
            @PathVariable Long boardId,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 12, sort = "postedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long viewerId = SecurityUtils.getCurrentMemberId();
        Slice<PostResDTO> response = postListService.getPostsByBoardAndCategory(boardId, category, viewerId, pageable);
        return ApiResponse.response(HttpStatus.OK, POSTS_BY_BOARD_READ.getMessage(), response);
    }
}
