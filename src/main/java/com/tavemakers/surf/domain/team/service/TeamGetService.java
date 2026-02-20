package com.tavemakers.surf.domain.team.service;

import com.tavemakers.surf.domain.team.entity.Team;
import com.tavemakers.surf.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamGetService {

    private final TeamRepository teamRepository;

    /** 기수별 팀 목록 조회 (멤버 포함) */
    public List<Team> getTeamsWithMembers(Integer generation) {
        return teamRepository.findTeamsWithMembers(generation);
    }

}
