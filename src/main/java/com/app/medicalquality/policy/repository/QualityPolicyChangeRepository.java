package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.QualityPolicyChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityPolicyChangeRepository extends JpaRepository<QualityPolicyChange, Long> {

    List<QualityPolicyChange> findByVersion_IdAndDeletedAtIsNullOrderByIdAsc(Long a_versionId);
}
