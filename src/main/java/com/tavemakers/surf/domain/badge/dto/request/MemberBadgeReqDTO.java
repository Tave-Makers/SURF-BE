package com.tavemakers.surf.domain.badge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class MemberBadgeReqDTO {

    @Schema(
            description = "배지를 부여하거나 회수할 회원 ID 목록",
            example = "[3, 7, 12]"
    )
    @NotEmpty
    private List<Long> memberIds;
}