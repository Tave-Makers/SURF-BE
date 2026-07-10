package com.tavemakers.surf.application.notification.usecase;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.domain.notification.entity.Platform;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.service.DeviceTokenRegisterService;
import com.tavemakers.surf.presentation.notification.dto.request.DeviceTokenReqDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DeviceTokenUsecase 단위 테스트.
 * register는 ReqDTO 해체 위임(대표 1건)만, disableTokens는 빈 입력 방어 분기를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenUsecaseTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenRegisterService deviceTokenRegisterService;

    @InjectMocks
    private DeviceTokenUsecase deviceTokenUsecase;

    @Test
    @DisplayName("register는 ReqDTO를 해체해 도메인 서비스로 위임한다")
    void register_ReqDTO를_해체해_위임한다() {
        DeviceTokenReqDTO dto = new DeviceTokenReqDTO("fcm-token", Platform.ANDROID);

        deviceTokenUsecase.register(1L, dto);

        then(deviceTokenRegisterService).should().register(1L, "fcm-token", Platform.ANDROID);
    }

    @Test
    @DisplayName("disableTokens는 빈 목록이면 repository를 호출하지 않는다")
    void disableTokens_빈목록이면_repository를_호출하지_않는다() {
        deviceTokenUsecase.disableTokens(List.of());

        then(deviceTokenRepository).should(never()).disableAllByTokenIn(anyList());
    }

    @Test
    @DisplayName("disableTokens는 토큰이 있으면 repository의 일괄 비활성화를 호출한다")
    void disableTokens_토큰이_있으면_일괄_비활성화를_호출한다() {
        List<String> tokens = List.of("t1", "t2");

        deviceTokenUsecase.disableTokens(tokens);

        then(deviceTokenRepository).should().disableAllByTokenIn(eq(tokens));
    }
}
