package com.tavemakers.surf.application.notification.usecase;

import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenUsecase {

    private final DeviceTokenRepository deviceTokenRepository;

    /** FCM 발송 실패로 확인된 무효 토큰 일괄 비활성화 */
    @Transactional
    public void disableTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        deviceTokenRepository.disableAllByTokenIn(tokens);
    }
}
