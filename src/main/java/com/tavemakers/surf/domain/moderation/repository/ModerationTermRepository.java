package com.tavemakers.surf.domain.moderation.repository;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ModerationTermRepository extends JpaRepository<ModerationTerm, Long> {

    boolean existsByTypeAndText(ModerationTermType type, String text);

    // 스냅숏 리빌드 + 종류별 목록 조회
    List<ModerationTerm> findAllByTypeOrderByTextAsc(ModerationTermType type);

    // 전체 목록 조회 (type 필터 미지정)
    List<ModerationTerm> findAllByOrderByTypeAscTextAsc();

    // 폴링 변경 감지용 — 항목이 없으면 null 이므로 Optional 로 받는다
    @Query("select max(t.updatedAt) from ModerationTerm t")
    Optional<LocalDateTime> findMaxUpdatedAt();

}
