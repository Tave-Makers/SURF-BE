package com.tavemakers.surf.domain.member.util;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class LogEventUtil {

    public static void logProfileUpdate(Map<String, Object> data, String status) {
        log.info("[LogEvent: member.profile_update] EventType=INFO Action='PATCH /v1/member/profile-update' Data={} Status={}",
                data, status);
    }
}
