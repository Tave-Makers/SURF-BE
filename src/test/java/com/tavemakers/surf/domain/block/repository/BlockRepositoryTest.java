package com.tavemakers.surf.domain.block.repository;

import com.tavemakers.surf.domain.block.entity.Block;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Block 저장소 계약 검증 — 단방향 레코드, 제약, 양방향 관계 검사.
 *
 * <p>차단은 안전 기능이라 저장소가 조용히 다르게 동작하면 곧바로 노출 사고가 된다.
 * 특히 "A가 B를 차단했다"가 "B도 A를 차단했다"로 새지 않는지를 고정한다.
 */
@DataJpaTest
class BlockRepositoryTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager em;

    private Member blocker;
    private Member blocked;

    @BeforeEach
    void setUp() {
        blocker = persistMember("blocker");
        blocked = persistMember("blocked");
    }

    @Test
    @DisplayName("차단은 단방향 — blocker 기준으로만 조회되고 반대 방향은 비어 있다")
    void 차단은_단방향이다() {
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        em.flush();

        assertThat(blockRepository.findBlockedIdsByBlockerId(blocker.getId()))
                .containsExactly(blocked.getId());
        assertThat(blockRepository.findBlockedIdsByBlockerId(blocked.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("existsBetween은 방향과 무관하게 true — 쪽지·알림 양방향 차단의 근거")
    void 관계_존재는_양방향으로_검사한다() {
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        em.flush();

        assertThat(blockRepository.existsBetween(blocker.getId(), blocked.getId())).isTrue();
        assertThat(blockRepository.existsBetween(blocked.getId(), blocker.getId())).isTrue();
    }

    @Test
    @DisplayName("관계가 없으면 existsBetween은 false")
    void 관계가_없으면_false다() {
        assertThat(blockRepository.existsBetween(blocker.getId(), blocked.getId())).isFalse();
    }

    @Test
    @DisplayName("차단이 0건이면 제외 대상 집합은 빈 Set — sentinel 치환은 BlockGetService의 책임")
    void 차단이_없으면_빈_집합이다() {
        assertThat(blockRepository.findBlockedIdsByBlockerId(blocker.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 방향 중복 차단은 unique 제약 위반")
    void 같은_방향_중복_차단은_막힌다() {
        blockRepository.saveAndFlush(Block.of(blocker.getId(), blocked.getId()));

        assertThatThrownBy(() -> blockRepository.saveAndFlush(Block.of(blocker.getId(), blocked.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("반대 방향 차단은 별개 레코드로 공존한다")
    void 반대_방향은_별개_레코드다() {
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(blocked.getId(), blocker.getId()));
        em.flush();

        assertThat(blockRepository.count()).isEqualTo(2);
        assertThat(blockRepository.findBlockedIdsByBlockerId(blocker.getId()))
                .containsExactly(blocked.getId());
        assertThat(blockRepository.findBlockedIdsByBlockerId(blocked.getId()))
                .containsExactly(blocker.getId());
    }

    @Test
    @DisplayName("자기 차단은 DB CHECK 제약으로도 막힌다 — 엔티티 검증을 우회해도 저장 불가")
    void 자기_차단은_DB에서도_막힌다() {
        // Block.of를 우회해 네이티브 insert로 직접 시도 (엔티티 검증의 최종 방어선 확인)
        Long selfId = blocker.getId();

        assertThatThrownBy(() -> {
            em.createNativeQuery("""
                            insert into block (blocker_id, blocked_id, created_at)
                            values (?, ?, current_timestamp)
                            """)
                    .setParameter(1, selfId)
                    .setParameter(2, selfId)
                    .executeUpdate();
            em.flush();
        }).hasStackTraceContaining("CHK_BLOCK_NOT_SELF");
    }

    @Test
    @DisplayName("방향별 삭제는 해당 방향만 지우고 반대 방향은 남긴다")
    void 방향별로_삭제된다() {
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(blocked.getId(), blocker.getId()));
        em.flush();

        Block target = blockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blocked.getId())
                .orElseThrow();
        blockRepository.delete(target);
        em.flush();

        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId()))
                .isFalse();
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(blocked.getId(), blocker.getId()))
                .isTrue();
    }

    /**
     * 엔티티가 Member를 scalar ID로만 들고 있어 Hibernate가 FK를 알지 못하므로,
     * ddl-auto로 만드는 H2 테스트 스키마에는 fk_block_blocker/blocked가 생성되지 않는다.
     * 따라서 여기서는 "관계가 남지 않는다"까지만 검증한다.
     * FK가 실제로 회원 hard delete를 막는지는 운영 DDL 적용 후 검증 DB에서 확인한다(§13.2 체크리스트).
     */
    @Test
    @DisplayName("제명 정리는 양방향 관계를 모두 지운다 — 운영에서 회원 hard delete의 FK 선행 조건")
    void 회원_관련_차단은_양방향으로_정리된다() {
        Member other = persistMember("other");
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(other.getId(), blocker.getId()));
        blockRepository.save(Block.of(other.getId(), blocked.getId()));
        em.flush();

        blockRepository.deleteAllRelatedTo(blocker.getId());
        em.flush();
        em.clear();

        assertThat(blockRepository.count()).isEqualTo(1);
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(other.getId(), blocked.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("내 차단 목록은 최신순 Slice로 조회된다")
    void 내_차단_목록은_최신순이다() {
        Member first = persistMember("first");
        Member second = persistMember("second");
        blockRepository.save(Block.of(blocker.getId(), first.getId()));
        blockRepository.save(Block.of(blocker.getId(), second.getId()));
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        em.flush();
        em.clear();

        Slice<Block> slice = blockRepository.findByBlockerIdOrderByCreatedAtDescIdDesc(
                blocker.getId(), PageRequest.of(0, 2));

        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue();
        // createdAt이 동일 시각으로 찍혀도 id DESC 보조 정렬로 순서가 확정된다
        List<Long> ids = slice.getContent().stream().map(Block::getId).toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("특정 회원 관련 전체 조회는 blocker/blocked 양쪽을 모두 포함한다")
    void 관리자_조회는_양방향을_포함한다() {
        Member other = persistMember("other");
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(other.getId(), blocker.getId()));
        em.flush();
        em.clear();

        Slice<Block> slice = blockRepository.findAllRelatedTo(blocker.getId(), PageRequest.of(0, 10));

        assertThat(slice.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("전체 조회는 회원 구분 없이 모든 관계를 최신순으로 준다 — 관리자 목록의 memberId 미지정 경로")
    void 전체_조회는_모든_관계를_포함한다() {
        Member other = persistMember("other");
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(other.getId(), blocked.getId()));
        blockRepository.save(Block.of(blocked.getId(), other.getId()));
        em.flush();
        em.clear();

        Slice<Block> slice = blockRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));

        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue();
        List<Long> ids = slice.getContent().stream().map(Block::getId).toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("countByBlockerId는 내가 등록한 차단만 센다")
    void 내가_등록한_차단만_센다() {
        blockRepository.save(Block.of(blocker.getId(), blocked.getId()));
        blockRepository.save(Block.of(blocked.getId(), blocker.getId()));
        em.flush();

        assertThat(blockRepository.countByBlockerId(blocker.getId())).isEqualTo(1);
    }

    /** 회원 ID가 IDENTITY 양수라는 sentinel(-1L) 전제를 고정한다 */
    @Test
    @DisplayName("회원 ID는 항상 양수 — sentinel(-1L)이 실제 회원과 겹치지 않는 전제")
    void 회원_ID는_양수다() {
        assertThat(blocker.getId()).isPositive();
        assertThat(blocked.getId()).isPositive();
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
