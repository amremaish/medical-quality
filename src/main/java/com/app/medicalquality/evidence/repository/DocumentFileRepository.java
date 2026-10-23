package com.app.medicalquality.evidence.repository;

import com.app.medicalquality.evidence.entity.DocumentFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    Optional<DocumentFile> findByIdAndDeletedAtIsNull(Long a_id);
}
