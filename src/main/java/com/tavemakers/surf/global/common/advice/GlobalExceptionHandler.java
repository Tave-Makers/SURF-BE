package com.tavemakers.surf.global.common.advice;

import com.tavemakers.surf.domain.auth.common.exception.EmailConflictException;
import com.tavemakers.surf.domain.letter.dto.request.LetterCreateReqDTO;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    // JSON 형식이 어긋난 경우 (유실, 형식X etc...)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logWarning(e, HttpStatus.BAD_REQUEST.value());
        return responseException(HttpStatus.BAD_REQUEST, e.getMessage(), null);
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
            } catch (Exception ignored) {}
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
        return responseException(errorCode.getStatus(), e.getMessage(), null);
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

}
