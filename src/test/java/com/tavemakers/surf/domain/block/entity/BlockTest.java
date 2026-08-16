package com.tavemakers.surf.domain.block.entity;

import com.tavemakers.surf.domain.block.exception.BlockSelfNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Block 생성 규칙 — DB CHECK 이전의 1차 방어선 */
class BlockTest {

    private static final Long BLOCKER_ID = 1L;
    private static final Long BLOCKED_ID = 2L;

    @Test
    @DisplayName("차단 관계를 생성한다")
    void 차단_관계를_생성한다() {
        Block block = Block.of(BLOCKER_ID, BLOCKED_ID);

        assertThat(block.getBlockerId()).isEqualTo(BLOCKER_ID);
        assertThat(block.getBlockedId()).isEqualTo(BLOCKED_ID);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void 자기_차단은_거부된다() {
        assertThatThrownBy(() -> Block.of(BLOCKER_ID, BLOCKER_ID))
                .isInstanceOf(BlockSelfNotAllowedException.class);
    }

    @Test
    @DisplayName("회원 ID는 null일 수 없다")
    void null_ID는_거부된다() {
        assertThatThrownBy(() -> Block.of(null, BLOCKED_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Block.of(BLOCKER_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
