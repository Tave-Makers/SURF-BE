package com.tavemakers.surf.application.notification.query;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.presentation.notification.dto.response.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.domain.notification.service.NotificationRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 알림 read-model 조립. member 도메인 조회 계약을 오케스트레이션하고 표현형(NotificationResDTO)을
 * 구성하므로 application 계층에 위치한다. 트랜잭션(readOnly) 경계는 호출자(NotificationUsecase)가 소유한다.
 */
@RequiredArgsConstructor
@Service
public class NotificationGetService {

    private final NotificationRepository notificationRepository;
    private final NotificationRenderService renderer;
    private final MemberGetService memberGetService;

    /** 회원의 알림 목록 조회 (카테고리별 필터링 가능, 무한스크롤) */
    public Slice<NotificationResDTO> getNotifications(Long memberId, NotificationCategory category, Pageable pageable) {

        Slice<Notification> notifications;

        if (category == null) {
            // 전체 알림
            notifications = notificationRepository.findByMemberIdOrderByIdDesc(memberId, pageable);
        } else {
            // 해당 카테고리의 타입 목록 추출
            List<NotificationType> types = Arrays.stream(NotificationType.values())
                    .filter(t -> t.getCategory() == category)
                    .toList();

            notifications = notificationRepository.findByMemberIdAndTypeInOrderByIdDesc(memberId, types, pageable);
        }

        List<NotificationResDTO> content = toDtoList(notifications.getContent());
        return new SliceImpl<>(content, pageable, notifications.hasNext());
    }

    /** 안 읽은 알림 존재 여부 조회 */
    public boolean hasUnread(Long memberId) {
        return notificationRepository.existsByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * 단건 변환 (프로필 이미지 없이)
     */
    public NotificationResDTO toDto(Notification n) {
        return new NotificationResDTO(
                n.getId(),
                n.getType(),
                n.getType().getCategory().name(),
                renderer.renderBody(n),
                renderer.renderDeeplink(n),
                n.isRead(),
                n.getCreatedAt(),
                null
        );
    }

    /**
     * 배치 변환 (N+1 방지, 프로필 이미지 포함)
     */
    public List<NotificationResDTO> toDtoList(List<Notification> notifications) {
        // 1. actorId 수집
        Set<Long> actorIds = notifications.stream()
                .map(renderer::extractActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 일괄 조회 (IN 쿼리)
        Map<Long, String> profileImageMap = memberGetService.getMembers(actorIds).stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        m -> m.getProfileImageUrl() != null ? m.getProfileImageUrl() : "",
                        (a, b) -> a
                ));

        // 3. DTO 변환
        return notifications.stream()
                .map(n -> toDto(n, profileImageMap))
                .toList();
    }

    private NotificationResDTO toDto(Notification n, Map<Long, String> profileImageMap) {
        Long actorId = renderer.extractActorId(n);
        String profileImageUrl = null;

        if (actorId != null && profileImageMap.containsKey(actorId)) {
            String url = profileImageMap.get(actorId);
            profileImageUrl = url.isEmpty() ? null : url;
        }

        return new NotificationResDTO(
                n.getId(),
                n.getType(),
                n.getType().getCategory().name(),
                renderer.renderBody(n),
                renderer.renderDeeplink(n),
                n.isRead(),
                n.getCreatedAt(),
                profileImageUrl
        );
    }
}
