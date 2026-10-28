package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.UserDepartmentAccess;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDepartmentAccessRepository extends JpaRepository<UserDepartmentAccess, Long> {

    @EntityGraph(attributePaths = {"department", "department.organization"})
    List<UserDepartmentAccess> findByUserIdAndDeletedAtIsNull(Long a_userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserDepartmentAccess access where access.user.id = :userId")
    void deleteByUserId(@Param("userId") Long a_userId);
}
