package com.app.medicalquality.auth.repository;

import com.app.medicalquality.auth.entity.UserPermissionOverrideEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionOverrideEventRepository extends JpaRepository<UserPermissionOverrideEvent, Long> {}
