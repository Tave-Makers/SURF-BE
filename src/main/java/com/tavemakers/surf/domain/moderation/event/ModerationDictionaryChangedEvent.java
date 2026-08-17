package com.tavemakers.surf.domain.moderation.event;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;

/**
 * 금칙어 사전 변경 이벤트 — 관리자 등록·삭제 시 발행한다.
 * 커밋 이후 리스너가 사전 전체를 다시 읽어 엔진 스냅숏을 교체한다.
 *
 * <p>리스너는 사전 전체를 리빌드하므로 아래 값은 어떤 편집이 갱신을 유발했는지
 * 남기기 위한 로그용 스냅샷이다 (엔티티를 담으면 커밋 이후 detach 문제가 생긴다).
 */
public record ModerationDictionaryChangedEvent(
        ModerationTermType type,
        String text
) {

    /** 변경된 사전 항목에서 로그용 이벤트를 만든다. */
    public static ModerationDictionaryChangedEvent from(ModerationTerm term) {
        return new ModerationDictionaryChangedEvent(term.getType(), term.getText());
    }

}
