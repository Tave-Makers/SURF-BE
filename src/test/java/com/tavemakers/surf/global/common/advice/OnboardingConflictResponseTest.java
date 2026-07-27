package com.tavemakers.surf.global.common.advice;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.exception.AccountIntegrationAvailableException;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** case B/C 응답 계약(message=한글, data.reason=코드) + case B issued 단계의 토큰 페이로드 병합 확인. */
class OnboardingConflictResponseTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(Mockito.mock(LogEventEmitter.class));

    @Test
    @DisplayName("case B (issued) — 409 / message=한글 / data.reason=ACCOUNT_INTEGRATION_REQUIRED + 토큰 페이로드")
    void caseB_issued() {
        ResponseEntity<ApiResponse<Map<String, Object>>> res = handler.handleAccountIntegrationAvailable(
                AccountIntegrationAvailableException.issued("tok-123", 1800L, "안내 문구"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiResponse<Map<String, Object>> body = res.getBody();
        assertThat(body.code()).isEqualTo(409);
        assertThat(body.message()).isEqualTo("이미 가입된 계정이 있습니다. 기존 계정으로 로그인 후 계정 통합을 완료해주세요.");
        assertThat(body.data())
                .containsEntry("reason", "ACCOUNT_INTEGRATION_REQUIRED")
                .containsEntry("integrationToken", "tok-123")
                .containsEntry("expiresInSeconds", 1800L)
                .containsEntry("guideMessage", "안내 문구");
    }

    @Test
    @DisplayName("case B (detected) — 토큰 미발급 단계면 data 는 reason 만 담고 토큰 키는 없다")
    void caseB_detected() {
        ResponseEntity<ApiResponse<Map<String, Object>>> res = handler.handleAccountIntegrationAvailable(
                AccountIntegrationAvailableException.detected(1L, 2L, 3L, Provider.KAKAO, "e@test.com", "01011112222"));

        assertThat(res.getBody().data())
                .containsEntry("reason", "ACCOUNT_INTEGRATION_REQUIRED")
                .doesNotContainKey("integrationToken");
    }

    @Test
    @DisplayName("case C — 409 / message=한글 / data.reason=ACCOUNT_CONFLICT_BLOCKED")
    void caseC() {
        ResponseEntity<ApiResponse<Map<String, String>>> res =
                handler.handleOnboardingConflict(new EmailAlreadyUsedException());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().data()).containsEntry("reason", "ACCOUNT_CONFLICT_BLOCKED");
    }
}
