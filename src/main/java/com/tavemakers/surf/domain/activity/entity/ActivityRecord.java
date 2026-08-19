package com.tavemakers.surf.domain.activity.entity;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRecord extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    private Long memberId;

    private Long teamId;

    @Enumerated(EnumType.STRING)
    private ActivityCategory category; // 대주제

    @Enumerated(EnumType.STRING)
    private ActivityType activityType; // 소주제 (활동 기록 명)

    @Enumerated(EnumType.STRING)
    private ScoreType scoreType; // 상점, 벌점 여부

    private LocalDate activityDate;

    @Column(precision = 19, scale = 1)
    private BigDecimal prefixSum = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP); // 누적합(정밀도 확보)

    @Column(precision = 19, scale = 1)
    private BigDecimal appliedScore = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

    @Column(nullable = false, columnDefinition = "TINYINT(1) default 0")
    private boolean isDeleted = false;

    // TODO 정적 팩토리 메서드
    public static ActivityRecord of(Long memberId, ActivityCategory category, ActivityType activityName, LocalDate activityDate, BigDecimal prefixSum, BigDecimal appliedScore) {
        return ActivityRecord.builder()
                .memberId(memberId)
                .category(category)
                .activityType(activityName)
                .activityDate(activityDate)
                .scoreType(activityName.getScoreType())
                .appliedScore(appliedScore)
                .prefixSum(prefixSum)
                .isDeleted(false)
                .build();
    }

    /** 개인 활동 점수 기록 */
    public static ActivityRecord ofPersonal(Long memberId, ActivityType activityName, LocalDate activityDate, BigDecimal prefixSum, BigDecimal appliedScore) {
        return ActivityRecord.builder()
                .memberId(memberId)
                .category(activityName.getCategory())
                .activityType(activityName)
                .activityDate(activityDate)
                .scoreType(activityName.getScoreType())
                .appliedScore(appliedScore)
                .prefixSum(prefixSum)
                .isDeleted(false)
                .build();
    }

    /** 팀 활동 점수 기록 */
    public static ActivityRecord ofTeam(Long teamId, ActivityType activityName, LocalDate activityDate, BigDecimal prefixSum) {
        return ActivityRecord.builder()
                .teamId(teamId)
                .category(activityName.getCategory())
                .activityType(activityName)
                .activityDate(activityDate)
                .scoreType(activityName.getScoreType())
                .appliedScore(BigDecimal.valueOf(activityName.getDelta()))
                .prefixSum(prefixSum)
                .isDeleted(false)
                .build();
    }

    /** 소프트 삭제 처리 */
    public void softDelete() {
        this.isDeleted = true;
    }

    /** 활동 유형 변경 시 실제 반영 점수(appliedScore)까지 함께 갱신한다. */
    public BigDecimal updateActivityType(ActivityType newActivityType, BigDecimal appliedScore) {
        BigDecimal oldAppliedScore = this.appliedScore;
        this.activityType = newActivityType;
        this.category = newActivityType.getCategory();
        this.scoreType = newActivityType.getScoreType();
        this.appliedScore = appliedScore;
        return this.appliedScore.subtract(oldAppliedScore);
    }

    /** 활동 날짜 변경 */
    public void updateActivityDate(LocalDate newActivityDate) {
        this.activityDate = newActivityDate;
    }

}
