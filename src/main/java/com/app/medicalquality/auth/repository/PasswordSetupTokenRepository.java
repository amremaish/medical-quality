package com.app.medicalquality.auth.repository;

import com.app.medicalquality.auth.entity.PasswordSetupToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordSetupToken token where token.tokenHash = :hash")
    Optional<PasswordSetupToken> lockByHash(@Param("hash") String a_hash);

    @Modifying(flushAutomatically = true)
    @Query("update PasswordSetupToken token set token.usedAt = :now where token.user.id = :userId and token.usedAt is null")
    int consumeUnused(@Param("userId") Long a_userId, @Param("now") LocalDateTime a_now);
}