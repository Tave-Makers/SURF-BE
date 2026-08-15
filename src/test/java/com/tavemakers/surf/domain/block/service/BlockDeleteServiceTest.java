package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.exception.BlockNotFoundException;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 차단 해제 도메인 서비스 — 해제는 (blocker → blocked) 한 방향만, 제명 정리는 양방향 전부.
 *
 * <p>준비 데이터는 BlockCreateService를 거치지 않고 리포지토리로 직접 넣어, 해제 테스트가
 * 등록 서비스의 동작에 묶이지 않게 한다.
 */
@DataJpaTest
class BlockDeleteServiceTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager em;

    private BlockDeleteService blockDeleteService;

    private Member blocker;
    private Member blocked;

    @BeforeEach
    void setUp() {
        blockDeleteService = new BlockDeleteService(blockRepository);
        blocker = persistMember("blocker");
        blocked = persistMember("blocked");
    }

    @Test
    @DisplayName("차단을 해제하면 해당 방향만 사라진다")
    void 해제는_한_방향만_지운다() {
        persistBlock(blocker.getId(), blocked.getId());
        persistBlock(blocked.getId(), blocker.getId());

        blockDeleteService.delete(blocker.getId(), blocked.getId());
        em.flush();

        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId()))
                .isFalse();
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocked.getId(), blocker.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("없는 차단 관계를 해제하면 404")
    void 없는_관계_해제는_404다() {
        assertThatThrownBy(() -> blockDeleteService.delete(blocker.getId(), blocked.getId()))
                .isInstanceOf(BlockNotFoundException.class);
    }

    @Test
    @DisplayName("반대 방향 차단만 있을 때 내가 해제하려 하면 404 — 상대의 차단은 내가 풀 수 없다")
    void 상대의_차단은_해제할_수_없다() {
        persistBlock(blocked.getId(), blocker.getId());

        assertThatThrownBy(() -> blockDeleteService.delete(blocker.getId(), blocked.getId()))
                .isInstanceOf(BlockNotFoundException.class);
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocked.getId(), blocker.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("제명 정리는 회원이 관련된 모든 차단을 지운다")
    void 제명_정리는_양방향을_지운다() {
        persistBlock(blocker.getId(), blocked.getId());
        persistBlock(blocked.getId(), blocker.getId());

        blockDeleteService.deleteAllRelatedTo(blocker.getId());
        em.flush();
        em.clear();

        assertThat(blockRepository.count()).isZero();
    }

    private void persistBlock(Long blockerId, Long blockedId) {
        blockRepository.save(Block.of(blockerId, blockedId));
    }

    private Member persistMember(String prefix) {
        long seed = System.nanoTime();
        Member member = Member.builder()
                .name("회원")
                .email(prefix + seed + "@test.com")
                .phoneNumber(String.valueOf(seed))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        em.persist(member);
        return member;
    }
}
