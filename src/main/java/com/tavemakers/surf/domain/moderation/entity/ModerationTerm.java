package com.tavemakers.surf.domain.moderation.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 금칙어 사전 항목 — 마스킹 엔진이 기동·갱신 시점에 통째로 읽어 트라이 스냅숏을 만든다.
 * 운영 중 사전의 진실 원천은 이 테이블이며, classpath 시드 파일은 최초 적재용이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "moderation_term",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_moderation_term_type_text", columnNames = {"type", "text"}))
public class ModerationTerm extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModerationTermType type;

    @Column(nullable = false, length = 100)
    private String text;

    @Builder
    private ModerationTerm(ModerationTermType type, String text) {
        this.type = type;
        this.text = text;
    }

    public static ModerationTerm of(ModerationTermType type, String text) {
        return ModerationTerm.builder()
                .type(type)
                .text(text)
                .build();
    }

}
