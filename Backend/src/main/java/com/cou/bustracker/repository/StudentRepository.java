package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByPhone(String phone);

    Optional<Student> findByEmail(String email);

    /** The JWT subject may be a phone or an email depending on the login flow. */
    @Query("SELECT s FROM Student s WHERE s.phone = :principal OR s.email = :principal")
    Optional<Student> findByPhoneOrEmail(@Param("principal") String principal);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    List<Student> findByIsActiveTrue();

    List<Student> findAllByOrderByCreatedAtDesc();

    long countByIsVerifiedTrue();

    long countByIsVerifiedFalse();

    long countByIsPhoneVerifiedTrue();

    long countByIsPhoneVerifiedTrueAndIsVerifiedFalse();
}
