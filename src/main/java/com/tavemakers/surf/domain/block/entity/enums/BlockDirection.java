package com.tavemakers.surf.domain.block.entity.enums;

/**
 * 관리자 차단 목록의 조회 방향. {@code memberId}가 주어졌을 때만 의미가 있다.
 */
public enum BlockDirection {

    /** 해당 회원이 차단한 관계 (blocker 기준) */
    BLOCKING,

    /** 해당 회원이 차단당한 관계 (blocked 기준) */
    BLOCKED,

    /** 해당 회원이 관련된 모든 관계 (양방향) */
    ALL
}
