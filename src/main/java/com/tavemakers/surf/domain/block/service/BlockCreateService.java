package com.tavemakers.surf.domain.block.service;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.exception.BlockAlreadyExistsException;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 차단 등록 도메인 로직. 트랜잭션 경계는 호출자(BlockUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class BlockCreateService {

    /** 운영 DDL(docs/sql/block-feature-create-block.sql)과 엔티티 @Table의 unique 제약 이름 */
    private static final String UNIQUE_CONSTRAINT_NAME = "uk_block_blocker_blocked";

    /** 예외 원인 체인 탐색 상한 — 순환 참조된 cause에서 무한 루프를 막는다 */
    private static final int MAX_CAUSE_DEPTH = 10;

    private final BlockRepository blockRepository;

    /**
     * 차단 관계 등록. 이미 같은 방향의 차단이 있으면 409로 끝낸다.
     *
     * <p>스크랩과 달리 중복 요청을 멱등 성공시키지 않는다. 차단은 사용자가 명시적으로 누르는
     * 안전 동작이라 "이미 차단됨"을 그대로 알려주는 편이 낫고, 같은 트랜잭션 안에서
     * 예외를 잡아 200으로 되돌리면 rollback-only 트랜잭션이 되어 커밋에 실패한다.
     */
    public Block create(Long blockerId, Long blockedId) {
        Block block = Block.of(blockerId, blockedId);

        // 순차 중복 요청은 여기서 걸러 아래 insert의 unique 위반 경로를 애초에 타지 않게 한다
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BlockAlreadyExistsException();
        }

        try {
            // 즉시 flush해 동시 중복 요청의 unique 위반을 커밋 전에 감지
            // (커밋 시점 예외는 UnexpectedRollbackException이 되어 409로 매핑되지 않는다)
            return blockRepository.saveAndFlush(block);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateBlock(e)) {
                throw new BlockAlreadyExistsException();
            }
            // unique 외의 제약 위반은 원인을 감추지 않고 그대로 전파한다.
            // 예: 차단 대상 회원이 그 사이 hard delete되어 fk_block_blocked를 위반한 경우,
            //     "이미 차단한 회원입니다"(409)로 바꾸면 사용자가 원인을 완전히 오해한다.
            throw e;
        }
    }

    /**
     * unique 제약(중복 차단) 위반인지 판별한다.
     *
     * <p>Spring은 JPA 경로에서 unique 위반도 {@code DuplicateKeyException}이 아니라 밋밋한
     * {@code DataIntegrityViolationException}으로 번역한다(원인 체인:
     * {@code DataIntegrityViolationException → org.hibernate...ConstraintViolationException → SQLException}).
     * 즉 Spring 추상 예외만으로는 UNIQUE와 FK를 구분할 수 없어 공급자 타입을 한 겹 벗겨야 한다.
     *
     * <p>Hibernate 6.5의 {@code ConstraintKind}는 UNIQUE/OTHER 둘뿐이라 FK·CHECK·NOT NULL이 모두
     * OTHER로 떨어진다. 방언이 kind를 채우지 못하는 경우에 대비해 제약 이름도 함께 확인하되,
     * H2가 {@code UK_BLOCK_BLOCKER_BLOCKED_INDEX_3}처럼 접미사를 붙여 보고하므로 완전 일치가 아닌
     * 접두사 일치로 본다.
     */
    private boolean isDuplicateBlock(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE
                        || isUniqueConstraintName(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isUniqueConstraintName(String constraintName) {
        return constraintName != null
                && constraintName.toLowerCase().startsWith(UNIQUE_CONSTRAINT_NAME);
    }
}
