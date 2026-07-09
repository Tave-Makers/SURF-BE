package com.tavemakers.surf.domain.feedback.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 500, nullable = false)
    private String content;

    @Column(length = 64, nullable = false)
    private String writerHash;  // 일 단위 해시

    @Builder
    private Feedback(String content, String writerHash) {
        this.content = content;
        this.writerHash = writerHash;
    }

    public static Feedback of(String content, String writerHash) {
        return Feedback.builder().content(content).writerHash(writerHash).build();
    }
}