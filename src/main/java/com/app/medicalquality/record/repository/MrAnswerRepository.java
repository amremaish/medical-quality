package com.app.medicalquality.record.repository;

import com.app.medicalquality.record.entity.MrAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MrAnswerRepository extends JpaRepository<MrAnswer, Long> {

    List<MrAnswer> findByRecord_Review_IdAndDeletedAtIsNull(Long a_reviewId);

    Optional<MrAnswer> findByRecordIdAndItemId(Long a_recordId, Long a_itemId);

    void deleteByRecord_Review_Id(Long a_reviewId);
}
