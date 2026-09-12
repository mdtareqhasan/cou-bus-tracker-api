package com.cou.bustracker.repository;

import com.cou.bustracker.entity.PhoneVerificationOtp;
import com.cou.bustracker.entity.PhoneVerificationOtp.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneVerificationOtpRepository extends JpaRepository<PhoneVerificationOtp, Long> {
    Optional<PhoneVerificationOtp> findByPhoneAndUserRole(String phone, UserRole userRole);
    void deleteByPhoneAndUserRole(String phone, UserRole userRole);
}
