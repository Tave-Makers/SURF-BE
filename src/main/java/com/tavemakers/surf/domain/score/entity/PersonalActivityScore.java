package com.tavemakers.surf.domain.score.entity;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
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

    public BigDecimal updateScore(BigDecimal score) {
        this.score = this.score.add(score);
        return this.score;
    }

    // TODO 상점 누적합, 벌점 누적합
    public BigDecimal updateScore(ActivityType activityType) {
        BigDecimal delta = BigDecimal.valueOf(activityType.getDelta());
        this.score = this.score.add(delta);

        if (activityType.isReward()) {
            this.rewardPrefixSum = this.rewardPrefixSum.add(delta);
            return this.score;
        }

        this.penaltyPrefixSum = this.penaltyPrefixSum.add(delta);
        return this.score;
    }

    public static PersonalActivityScore from(Member member) {
        return PersonalActivityScore.builder()
                .member(member)
                .score(member.isYB() ? BigDecimal.valueOf(100) : BigDecimal.valueOf(50)) // 기본 점수 100
                .rewardPrefixSum(BigDecimal.valueOf(0))
                .penaltyPrefixSum(BigDecimal.valueOf(0))
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

}
