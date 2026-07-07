package com.tavemakers.surf.domain.member.domain.service;

import com.tavemakers.surf.domain.member.presentation.dto.request.CareerCreateReqDTO;
import com.tavemakers.surf.domain.member.domain.entity.Career;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import com.tavemakers.surf.domain.member.domain.repository.CareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerCreateService {

    private final CareerRepository careerRepository;

    //경력 신규 생성
    @Transactional
    public void createCareer(Member member, List<CareerCreateReqDTO> dtos) {
        List<Career> newCareers = dtos.stream()
                .map(dto -> Career.of(dto, member))
                .toList();
        careerRepository.saveAll(newCareers);
    }
}
