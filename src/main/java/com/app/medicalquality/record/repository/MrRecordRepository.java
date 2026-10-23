package com.app.medicalquality.record.repository;

import com.app.medicalquality.record.entity.MrRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrRecordRepository extends JpaRepository<MrRecord, Long> {

    List<MrRecord> findByReviewIdAndDeletedAtIsNullOrderByCodeAsc(Long a_reviewId);

    Optional<MrRecord> findByReviewIdAndCodeAndDeletedAtIsNull(Long a_reviewId, String a_code);
}
