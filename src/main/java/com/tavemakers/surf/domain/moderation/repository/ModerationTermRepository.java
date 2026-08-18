package com.tavemakers.surf.domain.moderation.repository;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ModerationTermRepository extends JpaRepository<ModerationTerm, Long> {

    /** 같은 종류·표현의 항목이 이미 있는지 확인한다 — 중복 등록 차단용. */
    boolean existsByTypeAndText(ModerationTermType type, String text);

    /** 종류별 항목을 표현 오름차순으로 조회한다 — 스냅숏 리빌드 및 목록 조회에 쓴다. */
    List<ModerationTerm> findAllByTypeOrderByTextAsc(ModerationTermType type);

    /** 전체 항목을 종류·표현 오름차순으로 조회한다 (type 필터 미지정). */
    List<ModerationTerm> findAllByOrderByTypeAscTextAsc();

    /**
     * 사전 전체에서 가장 최근 수정 시각을 조회한다 — 폴링 기반 변경 감지용.
     *
     * <p>사전이 비어 있으면 집계 결과가 null 이므로 {@code Optional.empty()} 를 반환한다.
     * 호출자는 이 경우를 "변경 없음"이 아니라 "사전 없음"으로 구분해 다뤄야 한다.
     */
    @Query("select max(t.updatedAt) from ModerationTerm t")
    Optional<LocalDateTime> findMaxUpdatedAt();

}
