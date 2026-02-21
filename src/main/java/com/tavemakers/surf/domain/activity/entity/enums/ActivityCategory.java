package com.tavemakers.surf.domain.activity.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityCategory {
    STUDY_ON_PERSONAL("스터디(개인)"),
    PROJECT_ON_PERSONAL("프로젝트(개인)"),
    STUDY_ON_TEAM("스터디(팀)"),
    PROJECT_ON_TEAM("프로젝트(팀)"),
    REGULAR_SESSION("정규 행사"),
    ACTIVITY("활동"),
    ;

    final String displayName;

}
