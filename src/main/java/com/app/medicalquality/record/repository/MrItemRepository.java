package com.app.medicalquality.record.repository;

import com.app.medicalquality.record.entity.MrItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrItemRepository extends JpaRepository<MrItem, Long> {

    @EntityGraph(attributePaths = "section")
    List<MrItem> findByDeletedAtIsNullOrderBySection_SortOrderAscSortOrderAsc();

    Optional<MrItem> findByIdAndDeletedAtIsNull(Long a_id);

    long countByDeletedAtIsNull();
}
