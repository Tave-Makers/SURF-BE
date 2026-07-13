package com.tavemakers.surf.global.common.advice;

import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static com.tavemakers.surf.global.common.exception.ErrorCode.ACCESS_DENIED;
import static com.tavemakers.surf.global.common.exception.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.tavemakers.surf.global.common.exception.ErrorCode.MESSAGE_NOT_READABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(mock(LogEventEmitter.class));
    }

    @Test
    @DisplayName("AccessDeniedException은 403과 고정 메시지로 응답한다")
    void accessDenied_returns403() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleAccessDeniedException(new AccessDeniedException("Access Denied"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().message()).isEqualTo(ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("@PreAuthorize의 AuthorizationDeniedException도 AccessDeniedException 핸들러로 잡힌다")
    void authorizationDenied_isSubtypeOfAccessDenied() {
        AuthorizationDeniedException e =
                new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false));

        ResponseEntity<ApiResponse<Void>> res = handler.handleAccessDeniedException(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().message()).isEqualTo(ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("catch-all 핸들러는 원본 예외 메시지를 응답 본문에 노출하지 않는다")
    void catchAll_doesNotExposeExceptionMessage() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleException(new RuntimeException("jdbc:mysql://internal-host/surf 접속 실패"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().message()).isEqualTo(INTERNAL_SERVER_ERROR.getMessage());
        assertThat(res.getBody().message()).doesNotContain("jdbc:mysql");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException은 파서 내부 메시지 대신 고정 메시지로 응답한다")
    void messageNotReadable_returnsFixedMessage() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ApiResponse<Void>> res = handler.handleHttpMessageNotReadableException(e);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo(MESSAGE_NOT_READABLE.getMessage());
        assertThat(res.getBody().message()).doesNotContain("JSON parse error");
    }
}
