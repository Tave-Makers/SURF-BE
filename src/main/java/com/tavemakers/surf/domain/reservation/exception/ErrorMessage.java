package com.tavemakers.surf.domain.reservation.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 [예약정보]입니다."),
    RESERVATION_CANCELED_EXCEPTION(HttpStatus.BAD_REQUEST, "취소된 [예약정보]입니다."),
    RESERVATION_ALREADY_PUBLISHED(HttpStatus.CONFLICT, "이미 발행된 게시글의 [예약정보]는 변경할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;

}
