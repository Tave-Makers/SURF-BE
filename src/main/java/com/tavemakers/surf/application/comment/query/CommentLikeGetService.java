package com.tavemakers.surf.application.comment.query;

import com.tavemakers.surf.presentation.comment.dto.response.CommentLikeMemberResDTO;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 좋아요 조회 read-model 조립. 좋아요 개수/여부/누른 회원 목록의 표현형(DTO)을 구성한다.
 * 트랜잭션(readOnly) 경계를 소유하고 도메인 CommentLikeService에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class CommentLikeGetService {

    private final CommentLikeService commentLikeService;

    /** 댓글의 총 좋아요 수 */
    @Transactional(readOnly = true)
    public long countLikes(Long commentId) {
        return commentLikeService.countLikes(commentId);
    }

    /** 내가 해당 댓글에 좋아요 눌렀는지 여부 */
    @Transactional(readOnly = true)
    public boolean isLikedByMe(Long commentId, Long memberId) {
        return commentLikeService.isLikedByMe(commentId, memberId);
    }

    /** 특정 댓글에 좋아요를 누른 회원 목록 (ID, 이름, 프로필 이미지) 조회 */
    @Transactional(readOnly = true)
    public List<CommentLikeMemberResDTO> getMembersWhoLiked(Long commentId) {
        return commentLikeService.getMembersWhoLiked(commentId).stream()
                .map(CommentLikeMemberResDTO::from)
                .toList();
    }
}
