package com.tavemakers.surf.application.notification.usecase;

import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.service.DeviceTokenRegisterService;
import com.tavemakers.surf.presentation.notification.dto.request.DeviceTokenReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 디바이스 토큰 Usecase — 트랜잭션 경계를 소유하고 ReqDTO를 해체해 도메인 서비스에 전달한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class DeviceTokenUsecase {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenRegisterService deviceTokenRegisterService;

    /** 회원의 디바이스 토큰 등록 또는 갱신 */
    @Transactional
    public void register(Long memberId, DeviceTokenReqDTO dto) {
        deviceTokenRegisterService.register(memberId, dto.token(), dto.platform());
    }

    /** 로그아웃한 기기의 디바이스 토큰 삭제 — 본인 소유 토큰만 삭제된다 */
    @Transactional
    public void unregister(Long memberId, String token) {
        deviceTokenRepository.deleteByMemberIdAndToken(memberId, token);
    }

    /** FCM 발송 실패로 확인된 무효 토큰 일괄 비활성화 */
    @Transactional
    public void disableTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        deviceTokenRepository.disableAllByTokenIn(tokens);
    }
}
