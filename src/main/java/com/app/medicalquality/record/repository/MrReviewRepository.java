package com.app.medicalquality.record.repository;

import com.app.medicalquality.record.entity.MrReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrReviewRepository extends JpaRepository<MrReview, Long> {

    Optional<MrReview> findByIdAndDeletedAtIsNull(Long a_id);

    List<MrReview> findByDeletedAtIsNullOrderByReviewDateDesc();
}
