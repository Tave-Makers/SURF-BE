package com.tavemakers.surf.domain.letter.repository;

import com.tavemakers.surf.domain.letter.entity.Letter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    @Query("SELECT l FROM Letter l JOIN FETCH l.sender JOIN FETCH l.receiver WHERE l.sender.id = :senderId")
    Slice<Letter> findBySenderId(@Param("senderId") Long senderId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Letter l WHERE l.sender.id = :memberId OR l.receiver.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
