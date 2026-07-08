package com.tavemakers.surf.domain.feedback.presentation.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    FEEDBACK_CREATED("[피드백]이 성공적으로 생성되었습니다."),
    FEEDBACK_READ("[피드백]이 성공적으로 조회되었습니다.");

    private final String message;

}