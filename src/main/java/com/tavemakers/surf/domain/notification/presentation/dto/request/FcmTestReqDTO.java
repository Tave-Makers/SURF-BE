package com.tavemakers.surf.domain.notification.presentation.dto.request;

public record FcmTestReqDTO(
        Long memberId,
        String title,
        String body
) {}