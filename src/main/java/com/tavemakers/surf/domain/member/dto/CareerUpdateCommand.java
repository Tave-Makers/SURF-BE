package com.tavemakers.surf.domain.member.dto;

import java.time.LocalDate;

/** 경력 수정 커맨드 — presentation ReqDTO를 usecase에서 해체해 도메인으로 전달한다. */
public record CareerUpdateCommand(
        Long careerId,
        String companyName,
        String position,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isWorking
) {
}
