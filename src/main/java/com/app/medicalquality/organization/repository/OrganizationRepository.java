package com.app.medicalquality.organization.repository;

import com.app.medicalquality.organization.entity.Organization;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByDeletedAtIsNullOrderByNameAsc();

    Optional<Organization> findByNameAndDeletedAtIsNull(String a_name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select organization from Organization organization where organization.id = :id")
    Optional<Organization> lockById(@Param("id") Long a_id);
}
