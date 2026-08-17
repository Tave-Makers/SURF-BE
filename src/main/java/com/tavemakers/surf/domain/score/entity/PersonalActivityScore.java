package com.tavemakers.surf.domain.score.entity;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.ScoreType;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalActivityScore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(precision = 19, scale = 1)
    private BigDecimal rewardPrefixSum = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);;

    @Column(precision = 19, scale = 1)
    private BigDecimal penaltyPrefixSum = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);;

    @Column(precision = 19, scale = 1)
    private BigDecimal score = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

    /** 점수와 해당 유형(상점/벌점)의 누적합을 함께 가감 (수정/삭제 되돌리기 포함 모든 가감 경로 공용) */
    public BigDecimal applyDelta(BigDecimal delta, ScoreType scoreType) {
        this.score = this.score.add(delta);

        if (scoreType == ScoreType.REWARD) {
            this.rewardPrefixSum = this.rewardPrefixSum.add(delta);
            return this.score;
        }

        this.penaltyPrefixSum = this.penaltyPrefixSum.add(delta);
        return this.score;
    }

    /** 회원 유형(YB/OB)에 따른 실제 반영 점수를 계산해 누적 점수와 prefix sum에 반영한다. */
    public BigDecimal updateScore(ActivityType activityType) {
        return applyDelta(resolveAppliedScore(activityType), activityType.getScoreType());
    }

    /** 현재 회원 구분(YB/OB)을 기준으로 개인 활동점수를 초기 상태로 되돌린다. */
    public void resetForMember(Member member) {
        this.member = member;
        this.team = null;
        this.score = defaultScoreFor(member);
        this.rewardPrefixSum = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        this.penaltyPrefixSum = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }

    public static PersonalActivityScore from(Member member) {
        return PersonalActivityScore.builder()
                .member(member)
                .score(defaultScoreFor(member))
                .rewardPrefixSum(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                .penaltyPrefixSum(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                .build();
    }

    public static PersonalActivityScore from(Team team) {
        return PersonalActivityScore.builder()
                .team(team)
                .score(BigDecimal.valueOf(0))
                .rewardPrefixSum(BigDecimal.valueOf(0))
                .penaltyPrefixSum(BigDecimal.valueOf(0))
                .build();
    }

    public boolean isTeam() {
        return team != null;
    }

    public BigDecimal resolveAppliedScore(ActivityType activityType) {
        BigDecimal delta = BigDecimal.valueOf(activityType.getDelta()).setScale(1, RoundingMode.HALF_UP);
        if (member == null || member.isYB()) {
            return delta;
        }
        return delta.divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultScoreFor(Member member) {
        return BigDecimal.valueOf(member.isYB() ? 100 : 50).setScale(1, RoundingMode.HALF_UP);
    }

}
