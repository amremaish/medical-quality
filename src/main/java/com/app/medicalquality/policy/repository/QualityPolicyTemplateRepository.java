package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.QualityPolicyTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityPolicyTemplateRepository extends JpaRepository<QualityPolicyTemplate, Long> {

    Optional<QualityPolicyTemplate> findFirstByKindAndDeletedAtIsNull(String a_kind);

    Optional<QualityPolicyTemplate> findByIdAndDeletedAtIsNull(Long a_id);
}
