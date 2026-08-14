package com.tavemakers.surf.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReasonType {
    HATE_OR_ABUSE("혐오/차별/괴롭힘 표현입니다."),
    SPAM_OR_PROMOTION("스팸홍보/도배글입니다."),
    ILLEGAL_CONTENT("불법 정보를 포함하고 있습니다."),
    OBSCENE_CONTENT("음란물입니다."),
    UNPLEASANT_EXPRESSION("불쾌한 표현이 있습니다.");

    private final String description;
}
