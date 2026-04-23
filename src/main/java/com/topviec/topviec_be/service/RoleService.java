package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.response.ResRoleDefaultDTO;

import java.util.List;

public interface RoleService {
    List<ResRoleDefaultDTO> getDefaultRoles();
    ResRoleDefaultDTO toggleAction(Long roleId, String actionName, boolean enabled);
    List<ResRoleDefaultDTO> addActionToAllRoles(String name, String code);
    ResRoleDefaultDTO renameAction(Long roleId, String actionCode, String newName);
}