package com.tavemakers.surf.application.comment.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.exception.CommentNotFoundException;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.MentionResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 댓글 목록 read-model 조립. 댓글/멘션/좋아요 조회를 조합해 표현용 DTO를 구성한다.
 * 트랜잭션(readOnly) 경계는 호출부(CommentUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class CommentGetService {

    private final CommentRepository commentRepository;
    private final CommentMentionGetService commentMentionGetService;
    private final CommentLikeService commentLikeService;
    private final BlockGetService blockGetService;

    /** 댓글 ID로 엔티티 조회 */
    @Transactional(readOnly = true)
    public Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    /** 댓글 목록 조회 — 차단한 작성자의 댓글·대댓글은 제외한다 */
    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {

        // 0) 차단 작성자 집합 — Slice와 count에 반드시 같은 값을 넘긴다.
        //    다르면 "댓글 3개"라고 표시되는데 목록은 비어 보이는 불일치가 생긴다.
        Set<Long> excludedAuthorIds = blockGetService.getMyBlockedMemberIds(memberId);

        // 1) 댓글 Slice 조회
        Slice<Comment> commentSlice =
                commentRepository.findByPostIdExcludingAuthors(postId, excludedAuthorIds, pageable);

        // 2) 댓글 총 개수 조회
        long totalCount = commentRepository.countByPostIdExcludingAuthors(postId, excludedAuthorIds);

        // 3) 각 댓글 -> DTO 변환 (멘션 일괄 조회로 N+1 방지)
        List<Comment> comments = commentSlice.getContent();
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, List<MentionResDTO>> mentionMap =
                commentMentionGetService.getMentionsByCommentIds(commentIds);

        List<CommentResDTO> commentDtoList = comments.stream()
                .map(comment -> {
                    List<MentionResDTO> mentions = mentionMap.getOrDefault(comment.getId(), List.of());
                    boolean liked = memberId != null && commentLikeService.isLikedByMe(comment.getId(), memberId);
                    return CommentResDTO.from(comment, postId, mentions, liked);
                })
                .toList();

        // 4) CommentListResDTO로 감싸서 반환
        return new CommentListResDTO(
                commentDtoList,
                totalCount,
                commentSlice.hasNext()
        );
    }
}
