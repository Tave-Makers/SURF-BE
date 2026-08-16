package com.tavemakers.surf.application.block.query;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.entity.enums.BlockDirection;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 관리자 목록의 direction 라우팅 검증.
 *
 * <p>BLOCKING/BLOCKED를 바꿔 부르면 "이 회원이 차단한 목록"과 "이 회원이 차단당한 목록"이 뒤바뀐다.
 * 응답 형태가 같아 눈으로는 드러나지 않으므로 분기마다 대상 쿼리를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class BlockAdminGetServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private BlockAdminGetService blockAdminGetService;

    @Test
    @DisplayName("memberId가 없으면 전체를 최신순으로 조회한다")
    void 필터가_없으면_전체를_조회한다() {
        given(blockRepository.findAllByOrderByCreatedAtDescIdDesc(PAGEABLE)).willReturn(emptySlice());

        blockAdminGetService.getBlocks(null, null, PAGEABLE);

        then(blockRepository).should().findAllByOrderByCreatedAtDescIdDesc(PAGEABLE);
    }

    @Test
    @DisplayName("direction이 있어도 memberId가 없으면 전체 조회다 — direction은 memberId 종속이다")
    void memberId가_없으면_direction은_무시된다() {
        given(blockRepository.findAllByOrderByCreatedAtDescIdDesc(PAGEABLE)).willReturn(emptySlice());

        blockAdminGetService.getBlocks(null, BlockDirection.BLOCKING, PAGEABLE);

        then(blockRepository).should().findAllByOrderByCreatedAtDescIdDesc(PAGEABLE);
        then(blockRepository).should(never()).findByBlockerIdOrderByCreatedAtDescIdDesc(MEMBER_ID, PAGEABLE);
    }

    @Test
    @DisplayName("BLOCKING은 그 회원이 차단한 관계를 조회한다")
    void BLOCKING은_blocker_기준이다() {
        given(blockRepository.findByBlockerIdOrderByCreatedAtDescIdDesc(MEMBER_ID, PAGEABLE))
                .willReturn(emptySlice());

        blockAdminGetService.getBlocks(MEMBER_ID, BlockDirection.BLOCKING, PAGEABLE);

        then(blockRepository).should().findByBlockerIdOrderByCreatedAtDescIdDesc(MEMBER_ID, PAGEABLE);
    }

    @Test
    @DisplayName("BLOCKED는 그 회원이 차단당한 관계를 조회한다")
    void BLOCKED는_blocked_기준이다() {
        given(blockRepository.findByBlockedIdOrderByCreatedAtDescIdDesc(MEMBER_ID, PAGEABLE))
                .willReturn(emptySlice());

        blockAdminGetService.getBlocks(MEMBER_ID, BlockDirection.BLOCKED, PAGEABLE);

        then(blockRepository).should().findByBlockedIdOrderByCreatedAtDescIdDesc(MEMBER_ID, PAGEABLE);
    }

    @Test
    @DisplayName("ALL은 양방향을 조회한다")
    void ALL은_양방향이다() {
        given(blockRepository.findAllRelatedTo(MEMBER_ID, PAGEABLE)).willReturn(emptySlice());

        blockAdminGetService.getBlocks(MEMBER_ID, BlockDirection.ALL, PAGEABLE);

        then(blockRepository).should().findAllRelatedTo(MEMBER_ID, PAGEABLE);
    }

    @Test
    @DisplayName("memberId만 있고 direction이 없으면 ALL로 취급한다")
    void direction_기본값은_ALL이다() {
        given(blockRepository.findAllRelatedTo(MEMBER_ID, PAGEABLE)).willReturn(emptySlice());

        blockAdminGetService.getBlocks(MEMBER_ID, null, PAGEABLE);

        then(blockRepository).should().findAllRelatedTo(MEMBER_ID, PAGEABLE);
    }

    private Slice<Block> emptySlice() {
        return new SliceImpl<>(List.of(), PAGEABLE, false);
    }
}
