package com.tavemakers.surf.application.letter.query;

import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.repository.LetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LetterGetService {

    private final LetterRepository letterRepository;

    /** 발신한 쪽지 목록 페이징 조회 */
    @Transactional(readOnly = true)
    public Slice<Letter> getSentLetters(Long senderId, Pageable pageable) {
        return letterRepository.findBySenderId(senderId, pageable);
    }
}
