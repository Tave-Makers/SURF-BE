package com.tavemakers.surf.domain.group.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    GROUP_CREATED("[그룹]이 성공적으로 생성되었습니다.");


    private final String message;
}
