package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "그룹", description = "그룹 관련 CRUD API")
public class GroupDeleteController {

    private final GroupService groupService;

    @DeleteMapping("/v1/admin/groups/{groupId}")
    public void delete(@PathVariable Long groupId) {
        groupService.delete(groupId);
    }
}