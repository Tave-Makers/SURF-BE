package com.tavemakers.surf.application.moderation.query;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import com.tavemakers.surf.presentation.moderation.dto.response.ModerationTermResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 금칙어 사전 조회 — 관리자 목록 조회 전용.
 * 사전 규모가 600여 건이라 페이지네이션 없이 전량 반환한다.
 */
@Service
@RequiredArgsConstructor
public class ModerationTermGetService {

    private final ModerationTermRepository moderationTermRepository;

    /** 사전 항목 목록 조회 — type 이 null 이면 전체를 반환한다. */
    @Transactional(readOnly = true)
    public List<ModerationTermResDTO> getTerms(ModerationTermType type) {
        List<ModerationTerm> terms = (type == null)
                ? moderationTermRepository.findAllByOrderByTypeAscTextAsc()
                : moderationTermRepository.findAllByTypeOrderByTextAsc(type);

        return terms.stream()
                .map(ModerationTermResDTO::from)
                .toList();
    }

}
