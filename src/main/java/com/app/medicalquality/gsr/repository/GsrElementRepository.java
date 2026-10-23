package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.GsrElement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GsrElementRepository extends JpaRepository<GsrElement, Long> {

    @EntityGraph(attributePaths = {"standard", "standard.chapter"})
    List<GsrElement> findByDeletedAtIsNullOrderByStandard_Chapter_SortOrderAscStandard_SortOrderAscSortOrderAsc();

    List<GsrElement> findByStandardIdAndDeletedAtIsNullOrderBySortOrderAsc(Long a_standardId);

    Optional<GsrElement> findByIdAndDeletedAtIsNull(Long a_id);

    long countByDeletedAtIsNull();
}
