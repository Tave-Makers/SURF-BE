package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.request.GroupCreateReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tavemakers.surf.domain.group.controller.ResponseMessage.GROUP_CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "그룹", description = "그룹 관련 CRUD API")
public class GroupCreateController {

    private final GroupService groupService;

    @Operation(summary = "그룹 생성", description = "새로운 그룹을 생성합니다.")
    @PostMapping("/v1/admin/groups")
    public ApiResponse<GroupResDTO> create(
            @Valid @RequestBody GroupCreateReqDTO req) {

        GroupResDTO response = groupService.createGroup(req);
        return ApiResponse.response(HttpStatus.CREATED, GROUP_CREATED.getMessage(), response);
    }
}
