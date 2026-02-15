package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.request.GroupCreateReqDTO;
import com.tavemakers.surf.domain.group.dto.request.GroupUpdateReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/groups")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<GroupResDTO> getGroups(
            @RequestParam Integer generation,
            @RequestParam(defaultValue = "ALL") String type
    ) {
        return groupService.getGroups(generation, type);
    }

    @GetMapping("/{groupId}")
    public GroupDetailResDTO getGroupDetail(@PathVariable Long groupId) {
        return groupService.getGroupDetail(groupId);
    }

    @PostMapping
    public Long create(@RequestBody GroupCreateReqDTO req) {
        return groupService.create(req);
    }

    @PatchMapping("/{groupId}")
    public void update(@PathVariable Long groupId, @RequestBody GroupUpdateReqDTO req) {
        groupService.update(groupId, req);
    }

    @DeleteMapping("/{groupId}")
    public void delete(@PathVariable Long groupId) {
        groupService.delete(groupId);
    }
}