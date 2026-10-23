package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.Assessor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessorRepository extends JpaRepository<Assessor, Long> {

    Optional<Assessor> findByCodeAndDeletedAtIsNull(String a_code);

    List<Assessor> findByDeletedAtIsNullOrderByCodeAsc();

    long countByDeletedAtIsNull();
}
