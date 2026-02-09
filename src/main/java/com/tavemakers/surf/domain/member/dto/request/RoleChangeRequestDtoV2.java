package com.tavemakers.surf.domain.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RoleChangeRequestDtoV2(

        @Schema(description = "Role 변경 대상의 ID 목록")
        List<Long> memberIdList,

        @Schema(description = "변경할 역할", example = "MANAGER")
        @NotNull(message = "역할은 필수입니다.")
        MemberRole role
) {}
