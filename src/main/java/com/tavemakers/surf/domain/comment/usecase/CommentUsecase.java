package com.tavemakers.surf.domain.comment.usecase;

import com.tavemakers.surf.domain.comment.dto.request.CommentCreateReqDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.domain.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 Usecase */
@Service
@RequiredArgsConstructor
public class CommentUsecase {

    private final CommentService commentService;

    /** 댓글 생성 */
    @Transactional
    public CommentResDTO createComment(Long postId, Long memberId, CommentCreateReqDTO req) {
        return commentService.createComment(postId, memberId, req);
    }

    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(Long postId, Long commentId, Long memberId) {
        commentService.deleteComment(postId, commentId, memberId);
    }

    /** 댓글 목록 조회 */
    @Transactional(readOnly = true)
    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {
        return commentService.getComments(postId, pageable, memberId);
    }
}
