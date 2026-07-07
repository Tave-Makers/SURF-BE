package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.application.query.MemberGetService;
import com.tavemakers.surf.domain.notification.dto.response.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class NotificationGetService {

    private final NotificationRenderService renderer;
    private final MemberGetService memberGetService;

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
