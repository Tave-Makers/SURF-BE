package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import com.tavemakers.surf.domain.notification.entity.Platform;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 디바이스 토큰 등록 도메인 로직. DTO를 알지 못하며 원시값/엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(DeviceTokenUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class DeviceTokenRegisterService {

    private final DeviceTokenRepository deviceTokenRepository;

    /** 회원의 디바이스 토큰 등록 또는 갱신 */
    public void register(Long memberId, String token, Platform platform) {
        deviceTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> {
                            //토큰 소유자가 변경된 경우 업데이트
                            if (!existing.getMemberId().equals(memberId)) {
                                existing.updateOwner(memberId);
                            } else {
                                existing.touch();
                            }
                        },
                        () -> deviceTokenRepository.save(
                                DeviceToken.builder()
                                        .memberId(memberId)
                                        .token(token)
                                        .platform(platform)
                                        .build()
                        )
                );
    }
}
