package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.QualityPolicyVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityPolicyVersionRepository extends JpaRepository<QualityPolicyVersion, Long> {

    List<QualityPolicyVersion> findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(Long a_organizationId);

    Optional<QualityPolicyVersion> findByIdAndOrganization_IdAndDeletedAtIsNull(Long a_id, Long a_organizationId);
}
