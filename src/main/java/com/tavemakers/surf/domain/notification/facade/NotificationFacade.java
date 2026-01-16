package com.tavemakers.surf.domain.notification.facade;

import com.tavemakers.surf.domain.notification.dto.req.DeviceTokenReqDTO;
import com.tavemakers.surf.domain.notification.dto.res.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.service.DeviceTokenRegisterService;
import com.tavemakers.surf.domain.notification.service.FcmService;
import com.tavemakers.surf.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final DeviceTokenRegisterService deviceTokenRegisterService;
    private final FcmService fcmService;

    // NotificationService
    public List<NotificationResDTO> getNotifications(Long memberId, NotificationCategory category) {
        return notificationService.getNotifications(memberId, category);
    }

    public void markAsRead(Long notificationId, Long memberId) {
        notificationService.markAsRead(notificationId, memberId);
    }

    // DeviceTokenRegisterService
    public void registerDeviceToken(Long memberId, DeviceTokenReqDTO dto) {
        deviceTokenRegisterService.register(memberId, dto);
    }

    // FcmService
    public void sendPushToMember(Long memberId, String title, String body, Long notificationId) {
        fcmService.sendToMember(memberId, title, body, notificationId);
    }
}
