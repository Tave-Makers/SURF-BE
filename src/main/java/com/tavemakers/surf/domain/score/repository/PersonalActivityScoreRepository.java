package com.tavemakers.surf.domain.score.repository;

import com.tavemakers.surf.domain.score.entity.PersonalActivityScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalActivityScoreRepository extends JpaRepository<PersonalActivityScore, Long> {

    Optional<PersonalActivityScore> findByMemberId(Long memberId);

    List<PersonalActivityScore> findAllByMemberIdIn(List<Long> memberIds);

    List<PersonalActivityScore> findAllByTeamIdIn(List<Long> teamIds);

    @Query("SELECT p FROM PersonalActivityScore p JOIN FETCH p.member")
    Slice<PersonalActivityScore> findPersonalActivityScoreSlice(Pageable pageable);

    @Query("SELECT p FROM PersonalActivityScore p JOIN FETCH p.team")
    Slice<PersonalActivityScore> findTeamActivityScoreSlice(Pageable pageable);

}
