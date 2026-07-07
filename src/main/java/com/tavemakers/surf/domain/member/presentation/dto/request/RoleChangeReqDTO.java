package com.tavemakers.surf.domain.member.presentation.dto.request;

import com.tavemakers.surf.domain.member.domain.entity.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RoleChangeReqDTO(
        @Schema(description = "변경할 역할", example = "MANAGER")
        @NotNull(message = "역할은 필수입니다.")
        MemberRole role
) {}
