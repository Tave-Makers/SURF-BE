package com.tavemakers.surf.domain.member.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MemberBanReqDTO(
        @NotEmpty
        List<Long> memberIds
) {
}