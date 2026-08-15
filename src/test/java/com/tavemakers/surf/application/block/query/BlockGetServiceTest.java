package com.tavemakers.surf.application.block.query;

import com.tavemakers.surf.domain.block.entity.Block;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlockGetService의 용도별 계약 검증.
 *
 * <p>이 서비스의 세 메서드는 반환 타입이 같아 바꿔 써도 컴파일된다. 잘못 쓰면
 * <b>차단 0건인 사용자의 목록이 통째로 비거나</b>(빈 Set을 JPQL {@code not in}에 전달),
 * 단방향 숨김 정책이 양방향으로 바뀐다. 그 구분을 테스트로 고정한다.
 */
@DataJpaTest
class BlockGetServiceTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private EntityManager em;

    private BlockGetService blockGetService;

    private Member viewer;
    private Member target;

    @BeforeEach
    void setUp() {
        blockGetService = new BlockGetService(blockRepository);
        viewer = persistMember("viewer");
        target = persistMember("target");
    }

    @Test
    @DisplayName("차단 0건이어도 쿼리용 집합은 비어 있지 않다 — 빈 not in으로 목록이 통째로 비는 사고 방지")
    void 쿼리용_집합은_절대_비지_않는다() {
        Set<Long> excluded = blockGetService.getMyBlockedMemberIds(viewer.getId());

        assertThat(excluded).isNotEmpty();
        assertThat(excluded).containsExactly(-1L);
    }

    @Test
    @DisplayName("sentinel은 실제 회원과 겹치지 않아 아무도 제외하지 않는다")
    void sentinel은_실제_회원을_제외하지_않는다() {
        Set<Long> excluded = blockGetService.getMyBlockedMemberIds(viewer.getId());

        // 차단 0건인 사용자의 피드에서 어떤 작성자도 걸러지면 안 된다
        assertThat(excluded).doesNotContain(viewer.getId(), target.getId());
    }

    @Test
    @DisplayName("차단이 있으면 쿼리용 집합에 sentinel 없이 대상 ID만 담긴다")
    void 차단이_있으면_대상_ID만_담긴다() {
        blockRepository.save(Block.of(viewer.getId(), target.getId()));
        em.flush();

        assertThat(blockGetService.getMyBlockedMemberIds(viewer.getId()))
                .containsExactly(target.getId())
                .doesNotContain(-1L);
    }

    @Test
    @DisplayName("표기용 집합은 차단 0건이면 빈 Set — sentinel이 섞이면 contains 판정 의미가 흐려진다")
    void 표기용_집합은_비어_있을_수_있다() {
        assertThat(blockGetService.getMyBlockedIdsRaw(viewer.getId())).isEmpty();
    }

    @Test
    @DisplayName("표기용 집합의 contains로 isBlocked를 판정한다")
    void 표기용_집합으로_isBlocked를_판정한다() {
        blockRepository.save(Block.of(viewer.getId(), target.getId()));
        em.flush();

        Set<Long> blockedIds = blockGetService.getMyBlockedIdsRaw(viewer.getId());

        assertThat(blockedIds.contains(target.getId())).isTrue();
        assertThat(blockedIds.contains(viewer.getId())).isFalse();
    }

    @Test
    @DisplayName("existsBetween은 양방향, isBlockedByMe는 단방향 — 두 정책이 섞이지 않는다")
    void 상호작용은_양방향_콘텐츠는_단방향이다() {
        blockRepository.save(Block.of(viewer.getId(), target.getId()));
        em.flush();

        // 상호작용(쪽지·알림): 어느 쪽에서 물어도 차단
        assertThat(blockGetService.existsBetween(viewer.getId(), target.getId())).isTrue();
        assertThat(blockGetService.existsBetween(target.getId(), viewer.getId())).isTrue();

        // 콘텐츠 숨김: 차단한 사람에게만 적용되고 상대 피드에서 내 글은 보인다
        assertThat(blockGetService.isBlockedByMe(viewer.getId(), target.getId())).isTrue();
        assertThat(blockGetService.isBlockedByMe(target.getId(), viewer.getId())).isFalse();
        assertThat(blockGetService.getMyBlockedMemberIds(target.getId()))
                .doesNotContain(viewer.getId());
    }

    @Test
    @DisplayName("내 차단 목록은 내가 등록한 것만 — 나를 차단한 회원은 노출하지 않는다")
    void 내_차단_목록에_나를_차단한_회원은_없다() {
        blockRepository.save(Block.of(target.getId(), viewer.getId()));
        em.flush();
        em.clear();

        List<Block> myBlocks = blockGetService
                .getMyBlocks(viewer.getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(myBlocks).isEmpty();
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
