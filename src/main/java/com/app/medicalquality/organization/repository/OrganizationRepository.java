package com.app.medicalquality.organization.repository;

import com.app.medicalquality.organization.entity.Organization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByDeletedAtIsNullOrderByNameAsc();
}
