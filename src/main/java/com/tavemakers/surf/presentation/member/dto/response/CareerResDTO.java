package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Career;
import lombok.Builder;

import java.time.YearMonth;

@Builder
public record CareerResDTO(
        Long careerId,
        String companyName,
        String position,
        YearMonth startDate,
        YearMonth endDate,
        boolean isWorking
) {
    public static CareerResDTO from(Career career) {
        return CareerResDTO.builder()
                .careerId(career.getId())
                .companyName(career.getCompanyName())
                .position(career.getPosition())
                .startDate(YearMonth.from(career.getStartDate()))
                .endDate(career.getEndDate() != null ? YearMonth.from(career.getEndDate()) : null)
                .isWorking(career.isWorking())
                .build();
    }
}
