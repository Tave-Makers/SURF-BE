package com.tavemakers.surf.presentation.activity.dto.activityRecord.request;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory;
import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;
import com.tavemakers.surf.domain.activity.entity.enums.AppliedTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ActivityRecordReqDTOV2(
        List<Long> memberIdList,
        List<Long> teamIdList,
        ActivityCategory category, // MVP 1에서는 미정.

        @Schema(description = "적용 대상 - 팀 점수, 회원(개인)점수", example = "TEAM, INDIVIDUAL")
        AppliedTarget appliedTarget,

        @NotNull
        ActivityType activityName,

        @NotNull
        @Schema(description = "활동을 수행한 날짜, ex 지각한 날짜")
        LocalDate activityDate
) {

    @AssertTrue(message = "memberIdList와 teamIdList 둘 중 하나만 존재해야 하며, appliedTarget과 일치해야 합니다.")
    public boolean isAppliedIdListValid() {
        boolean hasMember = (memberIdList != null && !memberIdList.isEmpty());
        boolean hasTeam = (teamIdList != null && !teamIdList.isEmpty());

        if (!(hasMember ^ hasTeam)) {
            return false;
        }
        // 분기는 리스트 종류로 결정되므로, appliedTarget이 오면 리스트 종류와 일치해야 한다
        if (appliedTarget == AppliedTarget.TEAM) {
            return hasTeam;
        }
        if (appliedTarget == AppliedTarget.INDIVIDUAL) {
            return hasMember;
        }
        return true;
    }

    public boolean isTeam() {
        return teamIdList != null && !teamIdList.isEmpty();
    }

}