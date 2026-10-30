package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.QualityPolicyViolation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityPolicyViolationRepository extends JpaRepository<QualityPolicyViolation, Long> {

    List<QualityPolicyViolation> findByOrganization_IdAndDeletedAtIsNullAndResolvedAtIsNullOrderByIdDesc(
            Long a_organizationId);
}
