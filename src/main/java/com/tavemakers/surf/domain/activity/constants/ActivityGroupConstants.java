package com.tavemakers.surf.domain.activity.constants;

import com.tavemakers.surf.domain.activity.entity.enums.ActivityType;

import java.util.List;

import static com.tavemakers.surf.domain.activity.entity.enums.ActivityType.*;

public final class ActivityGroupConstants {

    private ActivityGroupConstants() {}

    public static final List<ActivityType> TAVE_ACTIVITIES_RECORDS = List.of(
            UPLOAD_INSTAGRAM_STORY,
            ENGAGE_TECH_SEMINAR,
            EARLY_BIRD
    );

    public static final List<ActivityType> BLOG_GROUP_RECORDS = List.of(
            WRITE_WIL,
            UPLOAD_TAVE_REVIEW
    );

    public static final List<ActivityType> SESSION_LATE_GROUP = List.of(
            SESSION_LATE_1_TO_10,
            SESSION_LATE_11_TO_20,
            SESSION_LATE_21_TO_30
    );

    public static final List<ActivityType> TEAM_LATE_GROUP = List.of(
            STUDY_LATE_6_TO_10,
            STUDY_LATE_11_TO_20,
            STUDY_LATE_21_TO_30,
            PROJECT_LATE_6_TO_10,
            PROJECT_LATE_11_TO_20,
            PROJECT_LATE_21_TO_30
    );

    public static final List<ActivityType> SESSION_ABSENCE_GROUP = List.of(
            SESSION_ABSENCE,
            SESSION_TRUANCY
    );

    public static final List<ActivityType> TEAM_ABSENCE_GROUP = List.of(
            STUDY_ABSENCE,
            PROJECT_ABSENCE
    );

}
