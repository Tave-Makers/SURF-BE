package com.tavemakers.surf.application.block.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.service.BlockCreateService;
import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import com.tavemakers.surf.presentation.block.dto.response.BlockSliceResDTO;
import com.tavemakers.surf.presentation.block.dto.response.BlockedMemberResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BlockUsecase 단위 테스트 — 도메인 서비스는 mock 처리하고, 검증 순서·위임 인자·DTO 매핑을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class BlockUsecaseTest {

    private static final Long ME = 1L;
    private static final Long TARGET = 2L;

    @Mock
    private BlockCreateService blockCreateService;

    @Mock
    private BlockDeleteService blockDeleteService;

    @Mock
    private BlockGetService blockGetService;

    @Mock
    private MemberGetService memberGetService;

    @InjectMocks
    private BlockUsecase blockUsecase;

    @Test
    @DisplayName("차단을 등록하면 대상 회원 요약과 차단 일시를 반환한다")
    void 차단_등록_응답을_매핑한다() {
        LocalDateTime blockedAt = LocalDateTime.of(2026, 8, 15, 10, 0);
        given(memberGetService.getMember(TARGET)).willReturn(member(TARGET, "홍길동", "https://img/2.png"));
        given(blockCreateService.create(ME, TARGET)).willReturn(block(101L, ME, TARGET, blockedAt));

        BlockedMemberResDTO response = blockUsecase.create(ME, TARGET);

        assertThat(response.memberId()).isEqualTo(TARGET);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.profileImageUrl()).isEqualTo("https://img/2.png");
        assertThat(response.blockedAt()).isEqualTo(blockedAt);
    }

    @Test
    @DisplayName("탈퇴한 회원은 차단할 수 없고 insert를 시도하지 않는다")
    void 탈퇴_회원은_차단할_수_없다() {
        Member withdrawn = member(TARGET, "탈퇴회원", null);
        ReflectionTestUtils.setField(withdrawn, "isDeleted", true);
        given(memberGetService.getMember(TARGET)).willReturn(withdrawn);

        assertThatThrownBy(() -> blockUsecase.create(ME, TARGET))
                .isInstanceOf(MemberNotFoundException.class);

        // 운영 FK는 RESTRICT라 없는 회원으로 insert하면 409가 아니라 FK 위반이 된다.
        // 검증이 insert보다 뒤로 밀리면 사용자에게 원인 불명 오류로 보인다.
        then(blockCreateService).should(never()).create(any(), any());
    }

    @Test
    @DisplayName("대상 회원 조회가 실패하면 insert를 시도하지 않는다")
    void 없는_회원은_차단할_수_없다() {
        given(memberGetService.getMember(TARGET)).willThrow(new MemberNotFoundException());

        assertThatThrownBy(() -> blockUsecase.create(ME, TARGET))
                .isInstanceOf(MemberNotFoundException.class);

        then(blockCreateService).should(never()).create(any(), any());
    }

    @Test
    @DisplayName("해제는 (나 → 대상) 방향으로 위임한다 — 인자가 뒤바뀌면 남의 차단을 지운다")
    void 해제_인자_순서를_지킨다() {
        blockUsecase.delete(ME, TARGET);

        then(blockDeleteService).should().delete(ME, TARGET);
    }

    @Test
    @DisplayName("내 차단 목록은 회원을 한 번에 조회해 매핑한다 — 행마다 조회하면 N+1이다")
    void 목록은_회원을_한번에_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime older = LocalDateTime.of(2026, 8, 14, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 8, 15, 10, 0);
        given(blockGetService.getMyBlocks(ME, pageable)).willReturn(new SliceImpl<>(
                List.of(block(101L, ME, 2L, newer), block(102L, ME, 3L, older)), pageable, false));
        given(memberGetService.getMembers(Set.of(2L, 3L))).willReturn(List.of(
                member(2L, "홍길동", "https://img/2.png"),
                member(3L, "김철수", null)));

        BlockSliceResDTO response = blockUsecase.getMyBlocks(ME, pageable);

        // 정확한 집합으로 검증한다 — 조회 1회(N+1 방지)와 대상 누락을 함께 잡는다
        then(memberGetService).should().getMembers(Set.of(2L, 3L));
        assertThat(response.content()).extracting(BlockedMemberResDTO::memberId).containsExactly(2L, 3L);
        assertThat(response.content()).extracting(BlockedMemberResDTO::name).containsExactly("홍길동", "김철수");
        assertThat(response.content().get(1).profileImageUrl()).isNull();
        assertThat(response.content().get(0).blockedAt()).isEqualTo(newer);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("차단이 없으면 회원 조회 없이 빈 목록을 반환한다 — 오류가 아니다")
    void 빈_목록은_오류가_아니다() {
        Pageable pageable = PageRequest.of(0, 20);
        given(blockGetService.getMyBlocks(ME, pageable)).willReturn(new SliceImpl<>(List.of(), pageable, false));

        BlockSliceResDTO response = blockUsecase.getMyBlocks(ME, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        then(memberGetService).should(never()).getMembers(anySet());
    }

    private Block block(Long id, Long blockerId, Long blockedId, LocalDateTime createdAt) {
        Block block = Block.of(blockerId, blockedId);
        ReflectionTestUtils.setField(block, "id", id);
        ReflectionTestUtils.setField(block, "createdAt", createdAt);
        return block;
    }

    private Member member(Long id, String name, String profileImageUrl) {
        Member member = Member.builder().name(name).profileImageUrl(profileImageUrl).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
