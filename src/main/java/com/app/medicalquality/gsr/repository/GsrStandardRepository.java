package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.GsrStandard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GsrStandardRepository extends JpaRepository<GsrStandard, Long> {

    Optional<GsrStandard> findByCodeAndDeletedAtIsNull(String a_code);

    List<GsrStandard> findByDeletedAtIsNullOrderBySortOrderAsc();
}
