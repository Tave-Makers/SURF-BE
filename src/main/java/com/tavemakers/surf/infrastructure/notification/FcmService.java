package com.tavemakers.surf.infrastructure.notification;

import com.google.firebase.messaging.*;
import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.application.notification.usecase.DeviceTokenUsecase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenUsecase deviceTokenUsecase;

    /** 회원에게 FCM 푸시 알림 전송 */
    public void sendToMember(Long memberId, String body, String deeplink, Long notificationId) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByMemberIdAndEnabledTrue(memberId);
        if (tokens.isEmpty()) return;

        List<String> tokenStrings = tokens.stream().map(DeviceToken::getToken).toList();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle("SURF")
                        .setBody(body)
                        .build())
                .putData("deepLink", deeplink)
                .putData("notificationId", String.valueOf(notificationId))
                // data payload는 나중에 딥링크/타입 붙일 때 추가하면 됨
                .addAllTokens(tokenStrings)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // 실패한 토큰 처리
            List<SendResponse> responses = response.getResponses();
            List<String> invalidTokens = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (r.isSuccessful()) continue;

                FirebaseMessagingException ex = r.getException();

                // 흔한 무효 토큰 케이스: UNREGISTERED / INVALID_ARGUMENT 등
                if (isInvalidToken(ex)) {
                    invalidTokens.add(tokenStrings.get(i));
                }
            }
            // 트랜잭션 밖에서 detached 엔티티를 수정하면 저장되지 않으므로,
            // 트랜잭션 경계(usecase) 안의 벌크 UPDATE로 비활성화한다.
            deviceTokenUsecase.disableTokens(invalidTokens);
        } catch (FirebaseMessagingException e) {
            // 여기서는 일단 런타임으로 던져서 로그/모니터링 하게 하는 게 보통
            throw new RuntimeException("FCM send failed", e);
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException ex) {
        // Firebase Admin SDK에서 MessagingErrorCode를 제공
        MessagingErrorCode code = ex.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}