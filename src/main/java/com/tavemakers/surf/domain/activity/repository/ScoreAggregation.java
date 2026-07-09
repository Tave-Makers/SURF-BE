package com.tavemakers.surf.domain.activity.repository;

import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;

import java.math.BigDecimal;

/** 상/벌점 집계 결과 projection 인터페이스 */
public interface ScoreAggregation {
    Long getGroupId();
    ScoreType getScoreType();
    BigDecimal getTotalScore();
}
