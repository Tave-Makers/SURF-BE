package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.post.entity.Post;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationUsecase {

    private final MemberGetService memberGetService;
    private final NotificationCreateService notificationCreateService;


    /**
     * 공지 게시글 발행 시
     * 이번 활동 기수(또는 활성 유저) 전체에게 알림 전송
     */
    @Transactional
    public void notifyNoticePost(Post post) {

        // 방어 로직 (이중 체크)
        if (!post.getBoard().isNotice()) {
            return;
        }

        // 1 알림 대상 조회
        List<Long> targetIds = memberGetService.getActiveMemberIds();

        if (targetIds.isEmpty()) {
            log.info("[NoticeNotification] no target members, postId={}", post.getId());
            return;
        }

        // 2 Notification 엔티티 일괄 생성 (N+1 방지)
        notificationCreateService.createAndSendBulk(
                targetIds,
                NotificationType.NOTICE,
                Map.of(
                        "boardName", post.getBoard().getName(),
                        "boardId", post.getBoard().getId(),
                        "postId", post.getId()
                )
        );

        log.info(
                "[NoticeNotification] sent to {} members, postId={}",
                targetIds.size(),
                post.getId()
        );
    }
}
