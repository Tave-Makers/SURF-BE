package com.tavemakers.surf.domain.member.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "프로필 수정 요청 DTO")
public record ProfileUpdateReqDTO(
        @Email
        String email,
        String university,
        String graduateSchool,

        @Size(max = 256)
        String selfIntroduction,

        @Size(max = 1024)
        String link,

        @Pattern(regexp = "^[0-9\\-]{8,15}$")
        String phoneNumber,
        Boolean phoneNumberPublic,

        String profileImageUrl,
        Boolean isProfileImageChanged,

        @Valid
        List<CareerCreateReqDTO> careersToCreate,

        @Valid
        List<CareerUpdateReqDTO> careersToUpdate,

        List<Long> careerIdsToDelete
) implements LogPropsProvider {
    public Map<String, Object> buildProps() {
        List<String> changedFields = new ArrayList<>();

        if (email != null && !email.isBlank()) {
            changedFields.add("email");
        }

        if (university != null && !university.isBlank()) {
            changedFields.add("university");
        }

        if (graduateSchool != null && !graduateSchool.isBlank()) {
            changedFields.add("graduateSchool");
        }

        if (selfIntroduction != null && !selfIntroduction.isBlank()) {
            changedFields.add("selfIntroduction");
        }

        if (link != null && !link.isBlank()) {
            changedFields.add("link");
        }

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            changedFields.add("phoneNumber");
        }

        if (phoneNumberPublic != null) {
            changedFields.add("phoneNumberPublic");
        }

        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            changedFields.add("profileImageUrl");
        }

        if (isProfileImageChanged != null) {
            changedFields.add("isProfileImageChanged");
        }

        return Map.of(
                "changed_fields", changedFields,

                "careers_created",
                careersToCreate == null
                        ? List.of()
                        : careersToCreate,

                "careers_created_count",
                careersToCreate == null
                        ? 0
                        : careersToCreate.size(),

                "careers_updated",
                careersToUpdate == null
                        ? List.of()
                        : careersToUpdate.stream()
                        .map(CareerUpdateReqDTO::careerId)
                        .toList(),

                "careers_updated_count",
                careersToUpdate == null
                        ? 0
                        : careersToUpdate.size(),

                "careers_deleted",
                careerIdsToDelete == null
                        ? List.of()
                        : careerIdsToDelete,

                "careers_deleted_count",
                careerIdsToDelete == null
                        ? 0
                        : careerIdsToDelete.size()
        );
    }
}