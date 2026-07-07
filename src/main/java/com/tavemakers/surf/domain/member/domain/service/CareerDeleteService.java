package com.tavemakers.surf.domain.member.domain.service;

import com.tavemakers.surf.domain.member.domain.entity.Career;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.repository.CareerRepository;
import com.tavemakers.surf.domain.member.domain.validator.CareerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CareerDeleteService {

    private final CareerRepository careerRepository;
    private final CareerValidator careerValidator;

    //경력 삭제
    @Transactional
    public void deleteCareer(Member member, List<Long> careerIds){

        Set<Long> requestedIds = new HashSet<>(careerIds);
        List<Career> careersToDelete = careerRepository.findAllByMemberAndIdIn(member, requestedIds);
        careerValidator.validateCareer(requestedIds, careersToDelete);

        careerRepository.deleteAll(careersToDelete);
    }
}
