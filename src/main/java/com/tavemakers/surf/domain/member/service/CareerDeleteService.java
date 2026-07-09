package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.member.entity.Career;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.CareerRepository;
import com.tavemakers.surf.domain.member.validator.CareerValidator;
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
