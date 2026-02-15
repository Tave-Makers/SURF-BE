package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.request.GroupUpsertReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.group.controller.ResponseMessage.GROUP_UPDATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "그룹", description = "그룹 관련 CRUD API")
public class GroupPutController {

    private final GroupService groupService;

    @Operation(summary = "그룹 수정", description = "기존 그룹 정보를 새로운 정보로 완전히 대체합니다.")
    @PutMapping("/v1/admin/groups/{groupId}")
    public ApiResponse<GroupResDTO> replace(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpsertReqDTO req
    ) {
        GroupResDTO response = groupService.updateGroup(groupId, req);
        return ApiResponse.response(HttpStatus.OK, GROUP_UPDATED.getMessage(), response);
    }
}
