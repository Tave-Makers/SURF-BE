package com.tavemakers.surf.domain.notification.controller;

import static com.tavemakers.surf.domain.notification.controller.ResponseMessage.FCM_TEST_SUCCESS;

import com.tavemakers.surf.domain.notification.dto.req.FcmTestReqDTO;
import com.tavemakers.surf.domain.notification.facade.NotificationFacade;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "FCM")
@RequestMapping("/v1/user/notifications")
public class FcmTestController {

    private final NotificationFacade notificationFacade;

    @Operation(
            summary = "FCM 푸시 알림 테스트",
            description = """
                특정 회원에게 FCM 푸시 알림을 테스트로 전송합니다.

                ⚠️ 이 API는 개발/테스트 용도로 사용되며,
                실제 서비스 로직에서는 Notification 생성 이후 자동으로 FCM이 전송됩니다.
                """
    )
    @PostMapping("/test/push")
    public ApiResponse<Void> testPush(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "FCM 푸시 테스트 요청 바디",
                    required = true
            )
            @RequestBody FcmTestReqDTO req
    ) {
        notificationFacade.sendPushToMember(req.memberId(), req.title(), req.body(), 1L);
        return ApiResponse.response(
                HttpStatus.OK,
                FCM_TEST_SUCCESS.getMessage()
        );
    }
}