package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.request.GroupUpdateReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupListResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/v1/admin/groups")
    public List<GroupListResDTO> getGroups(
            @RequestParam Integer generation,
            @RequestParam(defaultValue = "ALL") String type
    ) {
        return groupService.getGroups(generation, type);
    }

    @GetMapping("/v1/admin/groups/{groupId}")
    public GroupDetailResDTO getGroupDetail(@PathVariable Long groupId) {
        return groupService.getGroupDetail(groupId);
    }




    @PatchMapping("/v1/admin/groups/{groupId}")
    public void updateGroup(@PathVariable Long groupId, @RequestBody GroupUpdateReqDTO req) {
        groupService.update(groupId, req);
    }

    @DeleteMapping("/v1/admin/groups/{groupId}")
    public void deleteGroup(@PathVariable Long groupId) {
        groupService.delete(groupId);
    }
}