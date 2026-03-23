package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.entity.RoleDefault;
import com.topviec.topviec_be.repository.RoleDefaultRepository;
import com.topviec.topviec_be.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleDefaultRepository roleDefaultRepository;

    @Override
    public List<RoleDefault> getDefaultRoles() {
        return roleDefaultRepository.findAll();
    }
}