package com.tavemakers.surf.application.comment.query;

import com.tavemakers.surf.presentation.comment.dto.response.CommentListResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.CommentResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.MentionResDTO;
import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.repository.CommentRepository;
import com.tavemakers.surf.domain.comment.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 댓글 목록 read-model 조립. 댓글/멘션/좋아요 조회를 오케스트레이션하고 표현형(DTO)을 구성한다.
 * 트랜잭션(readOnly) 경계는 호출자(CommentUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class CommentGetService {

    private final CommentRepository commentRepository;
    private final CommentMentionGetService commentMentionGetService;
    private final CommentLikeService commentLikeService;

    /** 댓글 목록 조회 */
    public CommentListResDTO getComments(Long postId, Pageable pageable, Long memberId) {

        // 1) 댓글 Slice 조회
        Slice<Comment> commentSlice =
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);

        // 2) 댓글 총 개수 조회
        long totalCount = commentRepository.countByPostId(postId);

        // 3) 각 댓글 → DTO 변환 (멘션 일괄 조회로 N+1 방지)
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
