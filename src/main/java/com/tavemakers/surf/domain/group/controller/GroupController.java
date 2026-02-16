package com.tavemakers.surf.domain.group.controller;

import com.tavemakers.surf.domain.group.dto.request.GroupUpsertReqDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupDetailResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupListResDTO;
import com.tavemakers.surf.domain.group.dto.response.GroupResDTO;
import com.tavemakers.surf.domain.group.service.GroupService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.group.controller.ResponseMessage.GROUP_CREATED;
import static com.tavemakers.surf.domain.group.controller.ResponseMessage.GROUP_UPDATED;


@RestController
@RequiredArgsConstructor
@RequestMapping
public class GroupController {

    private final GroupService groupService;



    @DeleteMapping("/v1/admin/groups/{groupId}")
    public void deleteGroup(@PathVariable Long groupId) {
        groupService.delete(groupId);
    }
}