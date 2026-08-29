package com.tavemakers.surf.presentation.notification.controller;

import static com.tavemakers.surf.presentation.notification.controller.ResponseMessage.DEVICE_TOKEN_DELETED;

import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.presentation.notification.dto.request.DeviceTokenDeleteReqDTO;
import com.tavemakers.surf.application.notification.usecase.DeviceTokenUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "FCM")
@RequestMapping("/v1/user/notifications")
public class DeviceTokenDeleteController {

    private final DeviceTokenUsecase deviceTokenUsecase;

    /** 디바이스 FCM 토큰 삭제 */
    @Operation(
            summary = "디바이스 FCM 토큰 삭제",
            description = """
                로그인한 사용자의 디바이스 FCM 토큰을 서버에서 삭제합니다.

                🔹 로그아웃 시 반드시 호출되어야 합니다.
                   호출하지 않으면 로그아웃 후에도 해당 기기로 푸시 알림이 계속 전송됩니다.

                본인 소유 토큰만 삭제되며, 이미 없거나 타인 소유인 토큰은 무시됩니다(멱등).
                """
    )
    @DeleteMapping("/device-tokens")
    public ApiResponse<Void> deleteDeviceToken(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody DeviceTokenDeleteReqDTO dto
    ) {
        deviceTokenUsecase.unregister(user.getMember().getId(), dto.token());
        return ApiResponse.response(
                HttpStatus.OK,
                DEVICE_TOKEN_DELETED.getMessage()
        );
    }
}
