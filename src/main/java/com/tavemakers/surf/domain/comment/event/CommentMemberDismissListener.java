package com.tavemakers.surf.domain.comment.event;

import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.member.domain.event.MemberDismissedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 제명 시 comment 도메인 데이터 정리 — 제명 트랜잭션에 동기 참여한다 (D1).
 * 실패하면 제명 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 * (본인 작성 댓글·댓글좋아요는 게시글 삭제와 순서 의존이 있어 usecase가 직접 오케스트레이션)
 */
@Component
@RequiredArgsConstructor
public class CommentMemberDismissListener {

    private final CommentMentionRepository commentMentionRepository;

    /** 제명 회원이 멘션된 기록 삭제 */
    @EventListener
    public void onMemberDismissed(MemberDismissedEvent event) {
        commentMentionRepository.deleteAllByMentionedMemberId(event.memberId());
    }
}
