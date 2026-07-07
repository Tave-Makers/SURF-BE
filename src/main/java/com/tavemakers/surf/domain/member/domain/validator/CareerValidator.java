package com.tavemakers.surf.domain.member.domain.validator;

import com.tavemakers.surf.domain.member.domain.entity.Career;
import com.tavemakers.surf.domain.member.domain.exception.CareerNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CareerValidator {

    public void validateCareer(Set<Long> requestedIds,  List<Career> careersTo) {
        if(careersTo.size() != requestedIds.size()) {
            Set<Long> validIds = careersTo.stream()
                    .map(Career::getId)
                    .collect(Collectors.toSet());
            Set<Long> invalidIds = new HashSet<>(requestedIds);
            invalidIds.removeAll(validIds);
            throw new CareerNotFoundException("잘못되었거나 권한이 없는 경력 ID: " + invalidIds);
        }
    }
}
