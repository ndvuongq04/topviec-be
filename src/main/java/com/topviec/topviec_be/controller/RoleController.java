package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.entity.RoleDefault;
import com.topviec.topviec_be.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/default-permissions")
    public ResponseEntity<List<RoleDefault>> getDefaultRoles() {
        return ResponseEntity.ok(roleService.getDefaultRoles());
    }
}