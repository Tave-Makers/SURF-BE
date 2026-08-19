package com.tavemakers.surf.global.common.advice;

import com.tavemakers.surf.domain.auth.common.exception.EmailConflictException;
import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.domain.member.exception.AccountIntegrationAvailableException;
import com.tavemakers.surf.domain.member.exception.EmailAlreadyUsedException;
import com.tavemakers.surf.domain.member.exception.PhoneAlreadyUsedException;
import com.tavemakers.surf.global.common.exception.BaseException;
import com.tavemakers.surf.global.common.exception.ErrorCode;
import com.tavemakers.surf.global.common.exception.ErrorDetail;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tavemakers.surf.global.common.exception.ErrorCode.*;


@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LogEventEmitter logEventEmitter;
    private static final String LOG_FORMAT = "Class : {}, Code : {}, Message : {}";

    /** 동일 이메일 다른 provider 충돌 — existingProvider 필드를 응답 본문에 포함한다 (D6). */
    @ExceptionHandler(EmailConflictException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleEmailConflictException(EmailConflictException e) {
        logWarning(e, e.getStatus().value());
        Map<String, String> data = Map.of("existingProvider", e.getExistingProvider().name());
        return responseException(e.getStatus(), e.getMessage(), data);
    }

    /** 온보딩 통합 필요 감지 (case B, §3.5 / §3.6.2). message 는 한글 안내, 프론트는 data.reason 으로 분기한다. issued 단계면 1회성 integrationToken·만료·안내 문구를 data 에 함께 담는다. */
    @ExceptionHandler(AccountIntegrationAvailableException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccountIntegrationAvailable(AccountIntegrationAvailableException e) {
        logWarning(e, e.getStatus().value());
        Map<String, Object> data = new HashMap<>();
        data.put("reason", "ACCOUNT_INTEGRATION_REQUIRED");
        if (e.getIntegrationToken() != null) {
            data.put("integrationToken", e.getIntegrationToken());
            data.put("expiresInSeconds", e.getExpiresInSeconds());
            data.put("guideMessage", e.getGuideMessage());
        }
        return responseException(e.getStatus(), e.getMessage(), data);
    }

    /** 온보딩 부분 일치 차단 (case C, §3.5 / §3.6.2). message 는 한글 안내, 프론트는 data.reason 으로 분기한다. 어느 필드가 일치했는지는 노출하지 않는다(계정 존재 유추 방지). */
    @ExceptionHandler({EmailAlreadyUsedException.class, PhoneAlreadyUsedException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleOnboardingConflict(BaseException e) {
        logWarning(e, e.getStatus().value());
        Map<String, String> data = Map.of("reason", "ACCOUNT_CONFLICT_BLOCKED");
        return responseException(e.getStatus(), e.getMessage(), data);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        logWarning(e, e.getStatus().value());
        return responseException(e.getStatus(), e.getMessage(), null);
    }

    // Request Parameter 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        ErrorCode errorCode = PARAMETER_NOT_FOUND;
        logWarning(e, HttpStatus.BAD_REQUEST.value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    /** 요청 파라미터 타입 변환 실패를 400으로 응답한다 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        ErrorCode errorCode = METHOD_ARGUMENT_NOT_VALID;
        logWarning(e, errorCode.getStatus().value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    // JSON 형식이 어긋난 경우 (유실, 형식X etc...)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ErrorCode errorCode = MESSAGE_NOT_READABLE;
        logWarning(e, errorCode.getStatus().value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    /** 권한이 없는 요청에 대해 경로에 맞는 403 응답을 반환한다. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ACCESS_DENIED;
        logWarning(e, errorCode.getStatus().value());
        String message = isAdminRequest(request)
                ? "[관리자] 권한이 필요한 요청입니다."
                : errorCode.getMessage();
        return responseException(errorCode.getStatus(), message, null);
    }

    // @Valid 유효성 검증 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleMethodArgumentValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = METHOD_ARGUMENT_NOT_VALID;

        List<ErrorDetail> errors = e.getBindingResult()
                .getFieldErrors().stream()
                .map(fe -> ErrorDetail.of(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .toList();

        // 쪽지 전송 유효성 검증 실패 로그
        if (request != null && request.getRequestURI().contains("/letters")
                && e.getTarget() instanceof LetterCreateReqDTO req) {
            try {
                Long senderId = SecurityUtils.getCurrentMemberId();
                String failReason = e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                Map<String, Object> failedProps = new HashMap<>();
                failedProps.put("sender_id", senderId);
                if (req.receiverId() != null) failedProps.put("receiver_id", req.receiverId());
                failedProps.put("email_valid", e.getBindingResult().getFieldError("replyEmail") == null);
                failedProps.put("title_length", req.title() != null ? req.title().length() : 0);
                failedProps.put("content_length", req.content() != null ? req.content().length() : 0);
                failedProps.put("fail_reason", failReason);
                logEventEmitter.emitError("letter_send_validation_failed", failedProps, "쪽지 전송 유효성 검증 실패");
            } catch (Exception ex) {
                log.warn("쪽지 전송 유효성 검증 실패 로그 적재 실패: {}", ex.toString());
            }
        }

        logWarning(e, errorCode.getStatus().value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), errors);
    }

    // No Resource Error
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        ErrorCode errorCode = RESOURCE_NOT_FOUND;
        logWarning(e, errorCode.getStatus().value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ErrorCode errorCode = INTERNAL_SERVER_ERROR;
        Map<String, Object> errorProps = new HashMap<>();
        errorProps.put("error_code", "500");
        errorProps.put("error_msg", e.getMessage() != null ? e.getMessage() : "internal server error");
        logEventEmitter.emitError("any.error", errorProps, "서버 내부 오류");
        logError(e, errorCode.getStatus().value());
        return responseException(errorCode.getStatus(), errorCode.getMessage(), null);
    }

    private <T> ResponseEntity<ApiResponse<T>> responseException(HttpStatus status, String message, T data ) {
        ApiResponse<T> response = ApiResponse.response(status, message, data);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    private void logWarning(Exception e, int errorCode) {
        log.warn(e.getMessage(), e);
        log.warn(LOG_FORMAT, e.getClass().getSimpleName(), errorCode, e.getMessage());
    }

    private void logError(Exception e, int errorCode) {
        log.error(e.getMessage(), e);
        log.error(LOG_FORMAT, e.getClass().getSimpleName(), errorCode, e.getMessage());
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        return request != null && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/v1/admin/");
    }

}
