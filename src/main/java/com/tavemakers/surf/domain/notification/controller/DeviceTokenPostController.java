package com.tavemakers.surf.domain.notification.controller;

import static com.tavemakers.surf.domain.notification.controller.ResponseMessage.DEVICE_TOKEN_SUCCESS;
import static com.tavemakers.surf.domain.notification.controller.ResponseMessage.FCM_TEST_SUCCESS;

import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.notification.dto.req.DeviceTokenReqDTO;
import com.tavemakers.surf.domain.notification.facade.NotificationFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "FCM")
@RequestMapping("/v1/user/notifications")
public class DeviceTokenPostController {

    private final NotificationFacade notificationFacade;

    @Operation(
            summary = "디바이스 FCM 토큰 등록",
            description = """
                로그인한 사용자의 디바이스 FCM 토큰을 서버에 등록합니다.

                해당 토큰은 이후 알림(Notification) 발생 시
                푸시 알림(FCM)을 전송하기 위해 사용됩니다.

                🔹 웹 / 모바일 앱 최초 실행 시
                🔹 또는 FCM 토큰이 갱신되었을 때 호출되어야 합니다.
                """
    )
    @PostMapping("/device-tokens")
    public ApiResponse<Void> registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody DeviceTokenReqDTO dto
    ) {
        notificationFacade.registerDeviceToken(user.getMember().getId(), dto);
        return ApiResponse.response(
                HttpStatus.OK,
                DEVICE_TOKEN_SUCCESS.getMessage()
        );
    }
}