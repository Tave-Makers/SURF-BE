package com.tavemakers.surf.presentation.comment.controller;

import com.tavemakers.surf.presentation.comment.dto.response.CommentLikeMemberResDTO;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.presentation.comment.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "댓글 좋아요", description = "댓글 좋아요 / 취소 / 상태 관련 API")
public class CommentLikeGetController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "댓글 좋아요 개수 조회", description = "특정 댓글의 좋아요 개수 조회")
    @GetMapping("/v1/user/comments/{commentId}/like/count")
    public ApiResponse<Long> getLikeCount(@PathVariable Long commentId) {
        long count = commentLikeService.countLikes(commentId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_LIKE_COUNT_READ.getMessage(), count);
    }

    @Operation(summary = "댓글 좋아요 여부 조회", description = "현재 로그인 한 회원이 특정 댓글을 좋아요 눌렀는지 확인 (true면 좋아요 상태, false면 좋아요를 누르지 않은 상태)")
    @GetMapping("/v1/user/comments/{commentId}/like/me")
    public ApiResponse<Boolean> isLiked(@PathVariable Long commentId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        boolean liked = commentLikeService.isLikedByMe(commentId, memberId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_LIKE_STATUS_READ.getMessage(), liked);
    }

    @Operation(summary = "댓글 좋아요 누른 회원 목록 조회", description = "특정 댓글에 좋아요 누른 회원들의 ID, 이름, 프로필 이미지를 반환")
    @GetMapping("/v1/user/comments/{commentId}/like/members")
    public ApiResponse<List<CommentLikeMemberResDTO>> getLikedMembers(@PathVariable Long commentId) {
        List<CommentLikeMemberResDTO> likedMembers = commentLikeService.getMembersWhoLiked(commentId);
        return ApiResponse.response(HttpStatus.OK, COMMENT_LIKE_MEMBER_LIST_READ.getMessage(), likedMembers);
    }
}
