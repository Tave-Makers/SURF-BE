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
        List<String> updatedFields = new ArrayList<>();

        if (email != null && !email.isBlank()) {
            updatedFields.add("email");
        }
        if (university != null && !university.isBlank()) {
            updatedFields.add("university");
        }
        if (graduateSchool != null && !graduateSchool.isBlank()) {
            updatedFields.add("graduateSchool");
        }
        if (selfIntroduction != null && !selfIntroduction.isBlank()) {
            updatedFields.add("selfIntroduction");
        }
        if (link != null && !link.isBlank()) {
            updatedFields.add("link");
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            updatedFields.add("phoneNumber");
        }
        if (phoneNumberPublic != null) {
            updatedFields.add("phoneNumberPublic");
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            updatedFields.add("profileImageUrl");
        }
        if (isProfileImageChanged != null) {
            updatedFields.add("isProfileImageChanged");
        }

        List<CareerCreateReqDTO> createdCareers =
                careersToCreate == null ? List.of() : careersToCreate;

        List<Long> updatedCareers =
                careersToUpdate == null
                        ? List.of()
                        : careersToUpdate.stream()
                        .map(CareerUpdateReqDTO::careerId)
                        .toList();

        List<Long> deletedCareers =
                careerIdsToDelete == null ? List.of() : careerIdsToDelete;

        return Map.of(
                "updated_fields", updatedFields,

                "careers_create", createdCareers.isEmpty() ? 0 : 1,
                "careers_update", updatedCareers.isEmpty() ? 0 : 1,
                "careers_delete", deletedCareers.isEmpty() ? 0 : 1,

                "careers_created", createdCareers,
                "careers_updated", updatedCareers,
                "careers_deleted", deletedCareers
        );
    }
}
