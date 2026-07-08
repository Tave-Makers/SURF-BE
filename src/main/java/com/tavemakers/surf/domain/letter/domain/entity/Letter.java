package com.tavemakers.surf.domain.letter.domain.entity;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    indexes = {
        @Index(name = "idx_letter_sender_id", columnList = "sender_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Letter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long letterId;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(length = 100)
    private String sns;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Column(nullable = false, length = 100)
    private String replyEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    // 정적 생성 메서드
    public static Letter create(
            String title,
            String content,
            String sns,
            String replyEmail,
            Member sender,
            Member receiver
    ) {
        Letter letter = new Letter();
        letter.title = title;
        letter.content = content;
        letter.sns = sns;
        letter.replyEmail = replyEmail;
        letter.sender = sender;
        letter.receiver = receiver;
        return letter;
    }
}
