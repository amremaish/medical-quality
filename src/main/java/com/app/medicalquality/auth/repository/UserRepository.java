package com.app.medicalquality.auth.repository;

import com.app.medicalquality.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role", "assessor", "assessor.organization", "department", "department.organization", "organization"})
    Optional<User> findByUsername(String a_username);

    @EntityGraph(attributePaths = {"role", "organization"})
    List<User> findByOrganizationIsNotNullAndDeletedAtIsNull();
}
