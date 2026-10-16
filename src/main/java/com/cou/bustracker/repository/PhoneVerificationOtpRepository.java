package com.cou.bustracker.repository;

import com.cou.bustracker.entity.PhoneVerificationOtp;
import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneVerificationOtpRepository extends JpaRepository<PhoneVerificationOtp, Long> {
    Optional<PhoneVerificationOtp> findByPhoneAndUserRole(String phone, UserRole userRole);
    void deleteByPhoneAndUserRole(String phone, UserRole userRole);

    /** All rows whose OTP expired before {@code cutoff} (default: 24h ago) — used by cleanup. */
    List<PhoneVerificationOtp> findAllByExpiresAtBefore(LocalDateTime cutoff);

    /** Bulk delete in a single statement; returns the number of rows deleted. */
    @Modifying
    @Query("DELETE FROM PhoneVerificationOtp o WHERE o.expiresAt < :cutoff")
    int deleteAllExpiredSince(@Param("cutoff") LocalDateTime cutoff);
}
