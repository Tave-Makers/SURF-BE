package com.tavemakers.surf.domain.activity.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.tavemakers.surf.domain.activity.entity.enums.AppliedTarget.INDIVIDUAL;
import static com.tavemakers.surf.domain.activity.entity.enums.ScoreType.*;

@Getter
@AllArgsConstructor
public enum ActivityType {

    // REWARD 상점
    EARLY_BIRD("행사 얼리버드", 5, REWARD, INDIVIDUAL),
    ENGAGE_AFTER_PARTY("뒷풀이 참여", 5, REWARD, INDIVIDUAL),
    CREATE_FLASH_MEETUP("번개모임 주최", 10, REWARD, INDIVIDUAL),
    ENGAGE_FLASH_MEETUP("번개모임 참여", 5, REWARD, INDIVIDUAL),
    SHARE_INFORMATION_AGIT("아지트 정보 공유", 3, REWARD, INDIVIDUAL),
    ENGAGE_TECH_SEMINAR("기술 세미나 참여", 10, REWARD, INDIVIDUAL),
    PRESENT_OUTLINE("기획안 발표", 10, REWARD, INDIVIDUAL),
    CREATE_SOCIAL_CLUB("소모임 생성", 15, REWARD, INDIVIDUAL),
    ENGAGE_SOCIAL_CLUB("소모임 활동", 3, REWARD, INDIVIDUAL),
    WRITE_WIL("WIL 작성", 3, REWARD, INDIVIDUAL),
    UPLOAD_INSTAGRAM_STORY("인스타그램 스토리 업로드", 3, REWARD, INDIVIDUAL),
    UPLOAD_TAVE_REVIEW("TAVE 활동 후기 업로드", 20, REWARD, INDIVIDUAL),
    TEAM_LEADER("팀장 역할 수행", 10, REWARD, INDIVIDUAL),

    // PENALTY
    SESSION_ABSENCE("정규 세션 결석", -30,PENALTY, INDIVIDUAL),
    SESSION_TRUANCY("정규 세션 무단 결석", -100, PENALTY, INDIVIDUAL),

    SESSION_LATE("정규 세션 지각", 0, PENALTY, INDIVIDUAL),
    SESSION_LATE_1_TO_10("정규 세션 지각", -10, PENALTY, INDIVIDUAL),
    SESSION_LATE_11_TO_20("정규 세션 지각", -20, PENALTY, INDIVIDUAL),
    SESSION_LATE_21_TO_30("정규 세션 지각", -30, PENALTY, INDIVIDUAL),

    TEAM_LATE("스터디/프로젝트 지각", 0, PENALTY, INDIVIDUAL),
    STUDY_LATE_5_TO_9("스터디 지각", -5, PENALTY, INDIVIDUAL),
    STUDY_LATE_10_TO_19("스터디 지각", -10, PENALTY, INDIVIDUAL),
    STUDY_LATE_20_TO_29("스터디 지각", -15, PENALTY, INDIVIDUAL),
    STUDY_ABSENCE("스터디 결석", -30, PENALTY, INDIVIDUAL),

    PROJECT_LATE_5_TO_9("프로젝트 지각", -5, PENALTY, INDIVIDUAL),
    PROJECT_LATE_10_TO_19("프로젝트 지각", -10, PENALTY, INDIVIDUAL),
    PROJECT_LATE_20_TO_29("프로젝트 지각", -15, PENALTY,INDIVIDUAL),
    PROJECT_ABSENCE("프로젝트 결석", -30, PENALTY, INDIVIDUAL),
    TEAM_ABSENCE("스터디/프로젝트 결석", 0, PENALTY, INDIVIDUAL),

    NO_VOTE("투표 미참여", -15, PENALTY, INDIVIDUAL),
    DELAY_DEPOSIT("보증금 입금 지연", -5, PENALTY, INDIVIDUAL),
    NO_SHOW_AFTER_PARTY("뒷풀이 불참", -10, PENALTY, INDIVIDUAL),
    ;

    private String displayName;
    private Integer delta;
    private ScoreType scoreType;
    private AppliedTarget appliedTarget;

}
