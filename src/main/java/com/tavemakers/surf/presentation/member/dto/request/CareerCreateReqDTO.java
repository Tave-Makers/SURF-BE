package com.tavemakers.surf.presentation.member.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "경력 생성 요청 DTO")
public record CareerCreateReqDTO(
        @NotBlank
        String companyName,

        @NotBlank
        String position,

        @NotNull
        LocalDate startDate,
        LocalDate endDate,

        @NotNull
        Boolean isWorking
) implements LogPropsProvider {
    public Map<String, Object> buildProps() {
        List<String> changedFields = new ArrayList<>();

        if (companyName != null && !companyName.isBlank()) changedFields.add("companyName");
        if (position != null && !position.isBlank()) changedFields.add("position");
        if (startDate != null) changedFields.add("startDate");
        if (endDate != null) changedFields.add("endDate");
        if(isWorking !=null) changedFields.add("isWorking");

        return Map.of("changed_fields", changedFields);
    }
}
