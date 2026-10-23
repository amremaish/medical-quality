package com.app.medicalquality.role.service;

import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.repository.PermissionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(1)
public class PermissionSeedService implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    public PermissionSeedService(PermissionRepository a_permissionRepository) {
        this.permissionRepository = a_permissionRepository;
    }

    @Override
    public void run(ApplicationArguments a_args) {
        for (PermissionEnum permission : PermissionEnum.values()) {
            if (permissionRepository.findByName(permission.name()).isEmpty()) {
                Permission row = new Permission();
                row.setName(permission.name());
                permissionRepository.save(row);
            }
        }
    }
}
