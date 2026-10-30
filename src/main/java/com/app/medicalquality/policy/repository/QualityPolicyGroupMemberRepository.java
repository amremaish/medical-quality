package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.QualityPolicyGroupMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityPolicyGroupMemberRepository extends JpaRepository<QualityPolicyGroupMember, Long> {

    Optional<QualityPolicyGroupMember> findByOrganization_IdAndDeletedAtIsNull(Long a_organizationId);
}
