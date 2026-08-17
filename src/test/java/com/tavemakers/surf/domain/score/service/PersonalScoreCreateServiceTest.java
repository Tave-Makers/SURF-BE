package com.tavemakers.surf.domain.score.service;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import com.tavemakers.surf.domain.score.repository.PersonalActivityScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PersonalScoreCreateServiceTest {

    @Mock
    private PersonalActivityScoreRepository personalScoreRepository;

    @InjectMocks
    private PersonalScoreCreateService personalScoreCreateService;

    @Test
    @DisplayName("활동 기수 변경 시 기존 개인 활동 점수를 현재 회원 구분에 맞게 초기화한다")
    void resetPersonalScores_resetsExistingScores() {
        Member ybMember = member(1L, MemberType.YB);
        Member obMember = member(2L, MemberType.OB);

        PersonalActivityScore ybScore = PersonalActivityScore.builder()
                .member(ybMember)
                .score(BigDecimal.valueOf(42).setScale(1))
                .rewardPrefixSum(BigDecimal.valueOf(10).setScale(1))
                .penaltyPrefixSum(BigDecimal.valueOf(-68).setScale(1))
                .build();
        PersonalActivityScore obScore = PersonalActivityScore.builder()
                .member(obMember)
                .score(BigDecimal.valueOf(75).setScale(1))
                .rewardPrefixSum(BigDecimal.valueOf(30).setScale(1))
                .penaltyPrefixSum(BigDecimal.valueOf(-5).setScale(1))
                .build();

        given(personalScoreRepository.findAllByMemberIdIn(List.of(1L, 2L)))
                .willReturn(List.of(ybScore, obScore));

        personalScoreCreateService.resetPersonalScores(List.of(ybMember, obMember));

        assertThat(ybScore.getScore()).isEqualByComparingTo("100.0");
        assertThat(ybScore.getRewardPrefixSum()).isEqualByComparingTo("0.0");
        assertThat(ybScore.getPenaltyPrefixSum()).isEqualByComparingTo("0.0");

        assertThat(obScore.getScore()).isEqualByComparingTo("50.0");
        assertThat(obScore.getRewardPrefixSum()).isEqualByComparingTo("0.0");
        assertThat(obScore.getPenaltyPrefixSum()).isEqualByComparingTo("0.0");
    }

    private Member member(Long id, MemberType memberType) {
        Member member = Member.builder()
                .name("회원" + id)
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(memberType)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
