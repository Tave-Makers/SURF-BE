package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.exception.BlockAlreadyExistsException;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 제약 위반 → 예외 매핑 검증.
 *
 * <p>DataIntegrityViolationException은 unique 위반만 뜻하지 않는다. block 테이블에는 FK 2개,
 * CHECK 1개, NOT NULL 제약이 함께 걸려 있어 이를 전부 409("이미 차단한 회원입니다")로 바꾸면
 * 원인을 완전히 오해하게 만든다. 실제 시나리오는 다음과 같다.
 *
 * <ol>
 *   <li>차단 API가 대상 회원의 존재를 확인한다</li>
 *   <li>거의 동시에 관리자가 그 회원을 hard delete한다</li>
 *   <li>INSERT가 fk_block_blocked를 위반한다</li>
 *   <li>catch-all 매핑이면 "이미 차단함"으로 응답한다 — 실제 원인은 "대상이 사라짐"</li>
 * </ol>
 *
 * <p>H2 테스트 스키마에는 FK가 생성되지 않아(엔티티가 scalar ID 보유) 이 경로를 DB로 재현할 수 없으므로
 * 저장소를 mock으로 세워 제약 종류별 매핑만 결정적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BlockCreateConstraintMappingTest {

    private static final Long BLOCKER_ID = 1L;
    private static final Long BLOCKED_ID = 2L;

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private BlockCreateService blockCreateService;

    @Test
    @DisplayName("unique 위반은 중복 차단(409)으로 매핑된다")
    void unique_위반은_409로_매핑된다() {
        given(blockRepository.existsByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)).willReturn(false);
        given(blockRepository.saveAndFlush(any(Block.class)))
                .willThrow(dataIntegrityViolation(
                        ConstraintViolationException.ConstraintKind.UNIQUE, "uk_block_blocker_blocked"));

        assertThatThrownBy(() -> blockCreateService.create(BLOCKER_ID, BLOCKED_ID))
                .isInstanceOf(BlockAlreadyExistsException.class);
    }

    @Test
    @DisplayName("FK 위반은 409로 바뀌지 않고 그대로 전파된다 — 대상 회원 삭제 경합을 '이미 차단함'으로 감추지 않는다")
    void FK_위반은_중복차단으로_오인되지_않는다() {
        given(blockRepository.existsByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)).willReturn(false);
        given(blockRepository.saveAndFlush(any(Block.class)))
                .willThrow(dataIntegrityViolation(
                        ConstraintViolationException.ConstraintKind.OTHER, "fk_block_blocked"));

        assertThatThrownBy(() -> blockCreateService.create(BLOCKER_ID, BLOCKED_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BlockAlreadyExistsException.class);
    }

    @Test
    @DisplayName("제약 종류를 알 수 없으면(kind=OTHER, 이름 불명) 삼키지 않고 전파한다")
    void 알_수_없는_제약_위반은_전파된다() {
        given(blockRepository.existsByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)).willReturn(false);
        given(blockRepository.saveAndFlush(any(Block.class)))
                .willThrow(new DataIntegrityViolationException("원인 불명"));

        assertThatThrownBy(() -> blockCreateService.create(BLOCKER_ID, BLOCKED_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BlockAlreadyExistsException.class);
    }

    @Test
    @DisplayName("방언이 kind를 채우지 못해도 제약 이름으로 중복 차단을 판별한다")
    void kind가_없어도_제약이름으로_판별한다() {
        given(blockRepository.existsByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)).willReturn(false);
        given(blockRepository.saveAndFlush(any(Block.class)))
                .willThrow(dataIntegrityViolation(
                        ConstraintViolationException.ConstraintKind.OTHER, "UK_BLOCK_BLOCKER_BLOCKED"));

        assertThatThrownBy(() -> blockCreateService.create(BLOCKER_ID, BLOCKED_ID))
                .isInstanceOf(BlockAlreadyExistsException.class);
    }

    @Test
    @DisplayName("제약 이름에 방언 접미사가 붙어도 판별한다 — H2는 UK_..._INDEX_3 형태로 보고한다")
    void 접미사가_붙은_제약이름도_판별한다() {
        given(blockRepository.existsByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)).willReturn(false);
        given(blockRepository.saveAndFlush(any(Block.class)))
                .willThrow(dataIntegrityViolation(
                        ConstraintViolationException.ConstraintKind.OTHER,
                        "UK_BLOCK_BLOCKER_BLOCKED_INDEX_3"));

        assertThatThrownBy(() -> blockCreateService.create(BLOCKER_ID, BLOCKED_ID))
                .isInstanceOf(BlockAlreadyExistsException.class);
    }

    /** Spring이 Hibernate 제약 위반을 감싸 던지는 형태를 그대로 재현한다 */
    private DataIntegrityViolationException dataIntegrityViolation(
            ConstraintViolationException.ConstraintKind kind, String constraintName) {
        return new DataIntegrityViolationException(
                "제약 위반",
                new ConstraintViolationException("제약 위반", new SQLException(), kind, constraintName));
    }
}
