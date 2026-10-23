package com.app.medicalquality.role.repository;

import com.app.medicalquality.role.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String a_name);

    List<Role> findByDeletedAtIsNullOrderByNameAsc();
}
