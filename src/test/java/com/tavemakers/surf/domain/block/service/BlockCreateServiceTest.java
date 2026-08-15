package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.exception.BlockAlreadyExistsException;
import com.tavemakers.surf.domain.block.exception.BlockSelfNotAllowedException;
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
 * 차단 등록 도메인 서비스 — 순차 중복은 409, 자기 차단은 저장 전 거부.
 *
 * <p>동시 중복(saveAndFlush의 unique 위반 catch 경로)은 {@link BlockCreateConcurrencyTest},
 * 제약 종류별 예외 매핑은 {@link BlockCreateConstraintMappingTest}가 담당한다.
 */
@DataJpaTest
class BlockCreateServiceTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager em;

    private BlockCreateService blockCreateService;

    private Member blocker;
    private Member blocked;

    @BeforeEach
    void setUp() {
        blockCreateService = new BlockCreateService(blockRepository);
        blocker = persistMember("blocker");
        blocked = persistMember("blocked");
    }

    @Test
    @DisplayName("차단을 등록한다")
    void 차단을_등록한다() {
        Block block = blockCreateService.create(blocker.getId(), blocked.getId());

        assertThat(block.getId()).isNotNull();
        assertThat(block.getCreatedAt()).isNotNull();
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("중복 차단은 409로 끝난다 — 멱등 성공으로 되돌리지 않는다")
    void 중복_차단은_409다() {
        blockCreateService.create(blocker.getId(), blocked.getId());

        assertThatThrownBy(() -> blockCreateService.create(blocker.getId(), blocked.getId()))
                .isInstanceOf(BlockAlreadyExistsException.class);

        // 중복 시도 후에도 레코드는 1건만 남는다
        assertThat(blockRepository.countByBlockerId(blocker.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("자기 차단은 저장 시도 전에 거부된다")
    void 자기_차단은_거부된다() {
        assertThatThrownBy(() -> blockCreateService.create(blocker.getId(), blocker.getId()))
                .isInstanceOf(BlockSelfNotAllowedException.class);

        assertThat(blockRepository.count()).isZero();
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
