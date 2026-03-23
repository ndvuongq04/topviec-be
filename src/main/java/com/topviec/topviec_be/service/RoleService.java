package com.topviec.topviec_be.service;

import com.topviec.topviec_be.entity.RoleDefault;

import java.util.List;

public interface RoleService {
    List<RoleDefault> getDefaultRoles();
}