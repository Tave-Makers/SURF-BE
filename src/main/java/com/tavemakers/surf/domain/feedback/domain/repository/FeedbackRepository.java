package com.tavemakers.surf.domain.feedback.domain.repository;

import com.tavemakers.surf.domain.feedback.domain.entity.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    long countByWriterHash(String writerHash);

    Slice<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
