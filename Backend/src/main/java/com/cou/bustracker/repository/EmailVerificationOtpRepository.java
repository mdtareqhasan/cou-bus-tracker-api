package com.cou.bustracker.repository;

import com.cou.bustracker.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {
    Optional<EmailVerificationOtp> findByEmailAndUserRole(String email, EmailVerificationOtp.UserRole userRole);
    void deleteByEmailAndUserRole(String email, EmailVerificationOtp.UserRole userRole);
}
