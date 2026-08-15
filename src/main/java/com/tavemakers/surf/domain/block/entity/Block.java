package com.tavemakers.surf.domain.block.entity;

import com.tavemakers.surf.domain.block.exception.BlockSelfNotAllowedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 회원 간 차단 관계. blocker가 blocked를 차단한 사실을 나타내는 단방향 레코드다.
 *
 * <p>A→B와 B→A는 서로 다른 레코드이며, 한쪽을 지워도 반대 방향은 남는다.
 *
 * <p>변경되는 속성이 없는 immutable 관계이므로 {@code BaseEntity}(updatedAt 포함)를 상속하지 않고
 * {@code createdAt}만 둔다. 회원은 도메인 간 JPA 연관 대신 scalar ID로 보유하며,
 * 회원 정보 조회는 application 계층의 MemberGetService가 담당한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "block",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_block_blocker_blocked",
                columnNames = {"blocker_id", "blocked_id"}),
        indexes = @Index(name = "idx_block_blocked_id", columnList = "blocked_id"))
@Check(name = "chk_block_not_self", constraints = "blocker_id <> blocked_id")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long id;

    @Column(name = "blocker_id", nullable = false, updatable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false, updatable = false)
    private Long blockedId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Block(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    /** 차단 관계 생성 — 자기 차단은 DB CHECK 이전에 여기서 먼저 막는다 */
    public static Block of(Long blockerId, Long blockedId) {
        validate(blockerId, blockedId);
        return new Block(blockerId, blockedId);
    }

    private static void validate(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new IllegalArgumentException("차단 관계의 회원 ID는 null일 수 없습니다.");
        }
        if (blockerId.equals(blockedId)) {
            throw new BlockSelfNotAllowedException();
        }
    }
}
