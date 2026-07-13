package com.tavemakers.surf.presentation.notification.dto.request;

public record FcmTestReqDTO(
        Long memberId,
        String title,
        String body
) {}