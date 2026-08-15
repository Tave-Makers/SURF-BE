package com.tavemakers.surf.application.block.usecase;

import com.tavemakers.surf.application.block.query.BlockAdminGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.entity.enums.BlockDirection;
import com.tavemakers.surf.domain.block.event.BlockForceReleasedEvent;
import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.presentation.block.dto.response.BlockAdminResDTO;
import com.tavemakers.surf.presentation.block.dto.response.BlockAdminSliceResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BlockAdminUsecase 단위 테스트 — 양쪽 회원 조립과 강제 해제 감사 로그를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BlockAdminUsecaseTest {

    private static final Long ADMIN = 9L;

    @Mock
    private BlockAdminGetService blockAdminGetService;

    @Mock
    private BlockDeleteService blockDeleteService;

    @Mock
    private MemberGetService memberGetService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BlockAdminUsecase blockAdminUsecase;

    @Test
    @DisplayName("목록은 blocker·blocked 양쪽을 한 번의 조회로 채운다")
    void 양쪽_회원을_한번에_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime blockedAt = LocalDateTime.of(2026, 8, 15, 10, 0);
        given(blockAdminGetService.getBlocks(null, null, pageable)).willReturn(new SliceImpl<>(
                List.of(block(101L, 1L, 2L, blockedAt), block(102L, 2L, 3L, blockedAt)), pageable, true));
        given(memberGetService.getMembers(Set.of(1L, 2L, 3L))).willReturn(List.of(
                member(1L, "회원1"), member(2L, "회원2"), member(3L, "회원3")));

        BlockAdminSliceResDTO response = blockAdminUsecase.getBlocks(null, null, pageable);

        // 정확한 집합으로 검증한다 — blocker나 blocked 한쪽을 빠뜨려도 드러나야 한다
        then(memberGetService).should().getMembers(Set.of(1L, 2L, 3L));
        assertThat(response.content()).extracting(BlockAdminResDTO::blockId).containsExactly(101L, 102L);
        assertThat(response.content().get(0).blocker().memberId()).isEqualTo(1L);
        assertThat(response.content().get(0).blocker().name()).isEqualTo("회원1");
        assertThat(response.content().get(0).blocked().memberId()).isEqualTo(2L);
        assertThat(response.content().get(0).blockedAt()).isEqualTo(blockedAt);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("memberId·direction을 조회 서비스에 그대로 전달한다")
    void 필터를_그대로_위임한다() {
        Pageable pageable = PageRequest.of(0, 20);
        given(blockAdminGetService.getBlocks(7L, BlockDirection.BLOCKED, pageable))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        blockAdminUsecase.getBlocks(7L, BlockDirection.BLOCKED, pageable);

        then(blockAdminGetService).should().getBlocks(7L, BlockDirection.BLOCKED, pageable);
    }

    @Test
    @DisplayName("강제 해제는 삭제된 관계의 양쪽 회원을 실은 이벤트를 발행한다")
    void 강제_해제는_감사_이벤트를_발행한다() {
        given(blockDeleteService.deleteById(101L))
                .willReturn(block(101L, 1L, 2L, LocalDateTime.of(2026, 8, 15, 10, 0)));

        blockAdminUsecase.forceDelete(ADMIN, 101L);

        // 커밋 실패 시 "강제 해제" 성공 로그가 남지 않도록, usecase는 emit하지 않고 이벤트만 발행한다.
        // 실제 적재는 AFTER_COMMIT 리스너가 한다 (BlockForceReleasedLogListenerTest).
        ArgumentCaptor<BlockForceReleasedEvent> event =
                ArgumentCaptor.forClass(BlockForceReleasedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue())
                .isEqualTo(new BlockForceReleasedEvent(ADMIN, 101L, 1L, 2L));
    }

    private Block block(Long id, Long blockerId, Long blockedId, LocalDateTime createdAt) {
        Block block = Block.of(blockerId, blockedId);
        ReflectionTestUtils.setField(block, "id", id);
        ReflectionTestUtils.setField(block, "createdAt", createdAt);
        return block;
    }

    private Member member(Long id, String name) {
        Member member = Member.builder().name(name).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
