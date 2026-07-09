package com.tavemakers.surf.domain.activity.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScoreType {
    REWARD,   // 상점 (가점)
    PENALTY   // 벌점 (감점)
}
