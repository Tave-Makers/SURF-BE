package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupGenerationSectionResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "그룹", description = "그룹 관련 CRUD API")
public class GroupGetController {

    private final GroupService groupService;

    @Operation(summary = "그룹 목록 조회", description = "그룹 목록을 기수별로 구분하여 조회합니다. type 파라미터로 스터디/프로젝트 분리 가능 (ALL, STUDY, PROJECT)")
    @GetMapping("/v1/admin/groups")
    public List<GroupGenerationSectionResDTO> getGroups(
            @RequestParam(defaultValue = "ALL") String type
    ) {
        return groupService.getGroups(type);
    }

    @Operation(summary = "그룹 상세 조회", description = "그룹의 상세 정보를 조회합니다.")
    @GetMapping("/v1/admin/groups/{groupId}")
    public GroupDetailResDTO getDetail(@PathVariable Long groupId) {
        return groupService.getGroupDetail(groupId);
    }
}
