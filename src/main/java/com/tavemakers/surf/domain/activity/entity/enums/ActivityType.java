package com.tavemakers.surf.domain.activity.entity.enums;

import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityCategoryDetailResDTO;
import com.tavemakers.surf.domain.activity.dto.activityRecord.response.ActivityTypeDetailResDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

import static com.tavemakers.surf.domain.activity.entity.enums.ActivityCategory.*;
import static com.tavemakers.surf.domain.activity.entity.enums.AppliedTarget.INDIVIDUAL;
import static com.tavemakers.surf.domain.activity.entity.enums.AppliedTarget.TEAM;
import static com.tavemakers.surf.domain.activity.entity.enums.ScoreType.*;

@Getter
@AllArgsConstructor
public enum ActivityType {

    // REWARD 상점
    EARLY_BIRD("행사 얼리버드", 5, REWARD, INDIVIDUAL, REGULAR_SESSION),
    ENGAGE_AFTER_PARTY("뒷풀이 참여", 5, REWARD, INDIVIDUAL, ACTIVITY),
    CREATE_FLASH_MEETUP("번개모임 주최", 10, REWARD, INDIVIDUAL, ACTIVITY),
    ENGAGE_FLASH_MEETUP("번개모임 참여", 5, REWARD, INDIVIDUAL, ACTIVITY),
    SHARE_INFORMATION_AGIT("아지트 정보 공유", 3, REWARD, INDIVIDUAL, ACTIVITY),
    ENGAGE_TECH_SEMINAR("기술 세미나 참여", 10, REWARD, INDIVIDUAL, ACTIVITY),
    PRESENT_OUTLINE("기획안 발표", 10, REWARD, INDIVIDUAL, ACTIVITY),
    CREATE_SOCIAL_CLUB("소모임 생성", 15, REWARD, INDIVIDUAL, ACTIVITY),
    ENGAGE_SOCIAL_CLUB("소모임 활동", 3, REWARD, INDIVIDUAL, ACTIVITY),
    WRITE_WIL("WIL 작성", 3, REWARD, INDIVIDUAL, ACTIVITY),
    UPLOAD_INSTAGRAM_STORY("인스타그램 스토리 업로드", 3, REWARD, INDIVIDUAL, ACTIVITY),
    UPLOAD_TAVE_REVIEW("TAVE 활동 후기 업로드", 20, REWARD, INDIVIDUAL, ACTIVITY),
    TEAM_LEADER("팀장 역할 수행", 10, REWARD, INDIVIDUAL, ACTIVITY),

    // PENALTY
    SESSION_ABSENCE("정규 세션 결석", -30,PENALTY, INDIVIDUAL, REGULAR_SESSION),
    SESSION_TRUANCY("정규 세션 무단 결석", -100, PENALTY, INDIVIDUAL, REGULAR_SESSION),

    SESSION_LATE("정규 세션 지각", 0, PENALTY, INDIVIDUAL, REGULAR_SESSION),
    SESSION_LATE_1_TO_10("정규 세션 지각", -10, PENALTY, INDIVIDUAL, REGULAR_SESSION),
    SESSION_LATE_11_TO_20("정규 세션 지각", -20, PENALTY, INDIVIDUAL, REGULAR_SESSION),
    SESSION_LATE_21_TO_30("정규 세션 지각", -30, PENALTY, INDIVIDUAL, REGULAR_SESSION),

    TEAM_LATE("스터디/프로젝트 지각", 0, PENALTY, INDIVIDUAL, STUDY_ON_PERSONAL),
    STUDY_LATE_6_TO_10("스터디 지각", -5, PENALTY, INDIVIDUAL, STUDY_ON_PERSONAL),
    STUDY_LATE_11_TO_20("스터디 지각", -10, PENALTY, INDIVIDUAL, STUDY_ON_PERSONAL),
    STUDY_LATE_21_TO_30("스터디 지각", -15, PENALTY, INDIVIDUAL, STUDY_ON_PERSONAL),
    STUDY_ABSENCE("스터디 결석", -30, PENALTY, INDIVIDUAL, STUDY_ON_PERSONAL),

    PROJECT_LATE_6_TO_10("프로젝트 지각", -5, PENALTY, INDIVIDUAL, PROJECT_ON_PERSONAL),
    PROJECT_LATE_11_TO_20("프로젝트 지각", -10, PENALTY, INDIVIDUAL, PROJECT_ON_PERSONAL),
    PROJECT_LATE_21_TO_30("프로젝트 지각", -15, PENALTY,INDIVIDUAL, PROJECT_ON_PERSONAL),
    PROJECT_ABSENCE("프로젝트 결석", -30, PENALTY, INDIVIDUAL, PROJECT_ON_PERSONAL),
    TEAM_ABSENCE("스터디/프로젝트 결석", 0, PENALTY, INDIVIDUAL, PROJECT_ON_PERSONAL),

    NO_VOTE("투표 미참여", -15, PENALTY, INDIVIDUAL, ACTIVITY),
    DELAY_DEPOSIT("보증금 입금 지연", -5, PENALTY, INDIVIDUAL, ACTIVITY),
    NO_SHOW_AFTER_PARTY("뒷풀이 불참", -10, PENALTY, INDIVIDUAL, ACTIVITY),

    // TEAM_PENALTY
    PROJECT_LATE_6_TO_10_ON_TEAM("프로젝트 지각", -5, PENALTY, TEAM, PROJECT_ON_TEAM),
    PROJECT_LATE_11_TO_20_ON_TEAM("프로젝트 지각", -10, PENALTY, TEAM, PROJECT_ON_TEAM),
    PROJECT_LATE_21_TO_30_ON_TEAM("프로젝트 지각", -15, PENALTY, TEAM, PROJECT_ON_TEAM),

    STUDY_LATE_6_TO_10_ON_STUDY("스터디 지각", -5, PENALTY, TEAM, STUDY_ON_TEAM),
    STUDY_LATE_11_TO_20_ON_STUDY("스터디 지각", -10, PENALTY, TEAM, STUDY_ON_TEAM),
    STUDY_LATE_21_TO_30_ON_STUDY("스터디 지각", -15, PENALTY, TEAM, STUDY_ON_TEAM),

    LATE_CLERK_UPLOAD_ON_TEAM("서기 업로드 지각", -10, PENALTY, TEAM, PROJECT_ON_TEAM),
    LATE_PROGRESS_TABLE_UPLOAD_ON_TEAM("진행표 업로드 지각", -10, PENALTY, TEAM, PROJECT_ON_TEAM),
    DO_NOT_UPLOAD_CLERK_ON_TEAM("서기 미제출", -30, PENALTY, TEAM, PROJECT_ON_TEAM),
    DO_NOT_UPLOAD_PROGRESS_TABLE_ON_TEAM("진행표 미제출", -30, PENALTY, TEAM, PROJECT_ON_TEAM),
    LATE_UPLOAD_CERTIFICATION_PHOTO_ON_TEAM("사진 업로드 지각", -5, PENALTY, TEAM, PROJECT_ON_TEAM),
    LATE_SCHEDULE_ALERT_ON_TEAM("일정 공지 지각", -5, PENALTY, TEAM, PROJECT_ON_TEAM),
    LATE_SUBMIT_FINAL_PRODUCT_ON_TEAM("결과물 제출 지각", -30, PENALTY, TEAM, PROJECT_ON_TEAM),
    DO_NOT_ALERT_ABSENCE_OF_PERSONAL_REASON_ON_TEAM("개인 사유로 인한 결석 미공지", -10, PENALTY, TEAM, PROJECT_ON_TEAM),
    DO_NOT_ALERT_TARDINESS_OF_PERSONAL_REASON_ON_TEAM("개인 사유로 인한 지각 미공지", -10, PENALTY, TEAM, PROJECT_ON_TEAM),

    LATE_CLERK_UPLOAD_ON_STUDY("서기 업로드 지각", -10, PENALTY, TEAM, STUDY_ON_TEAM),
    LATE_PROGRESS_TABLE_UPLOAD_ON_STUDY("진행표 업로드 지각", -10, PENALTY, TEAM, STUDY_ON_TEAM),
    DO_NOT_UPLOAD_CLERK_ON_STUDY("서기 미제출", -30, PENALTY, TEAM, STUDY_ON_TEAM),
    DO_NOT_UPLOAD_PROGRESS_TABLE_ON_STUDY("진행표 미제출", -30, PENALTY, TEAM, STUDY_ON_TEAM),
    LATE_UPLOAD_CERTIFICATION_PHOTO_ON_STUDY("사진 업로드 지각", -5, PENALTY, TEAM, STUDY_ON_TEAM),
    LATE_SCHEDULE_ALERT_ON_STUDY("일정 공지 지각", -5, PENALTY, TEAM, STUDY_ON_TEAM),
    LATE_SUBMIT_FINAL_PRODUCT_ON_STUDY("결과물 제출 지각", -30, PENALTY, TEAM, STUDY_ON_TEAM),
    DO_NOT_ALERT_ABSENCE_OF_PERSONAL_REASON_ON_STUDY("개인 사유로 인한 결석 미공지", -10, PENALTY, TEAM, STUDY_ON_TEAM),
    DO_NOT_ALERT_TARDINESS_OF_PERSONAL_REASON_ON_STUDY("개인 사유로 인한 지각 미공지", -10, PENALTY, TEAM, STUDY_ON_TEAM),
    ;

    private String displayName;
    private Integer delta;
    private ScoreType scoreType;
    private AppliedTarget appliedTarget;
    private ActivityCategory category;

    public ActivityTypeDetailResDTO toDto() {
        return ActivityTypeDetailResDTO.of(this);
    }

    public boolean isReward() {
        return scoreType.equals(REWARD);
    }

    /** 특정 카테고리에 속한 모든 활동 종류(ActivityType) 조회 */
    public static ActivityCategoryDetailResDTO getDtoListByCategory(ActivityCategory category) {
        List<ActivityTypeDetailResDTO> collect = Arrays.stream(ActivityType.values())
                .filter(activity -> activity.getCategory() == category)
                .map(ActivityType::toDto)
                .toList();
        return ActivityCategoryDetailResDTO.of(category, collect);
    }

}
