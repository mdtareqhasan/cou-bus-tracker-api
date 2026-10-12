package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByPhone(String phone);

    Optional<Teacher> findByEmail(String email);

    /** The JWT subject may be a phone or an email depending on the login flow. */
    @Query("SELECT t FROM Teacher t WHERE t.phone = :principal OR t.email = :principal")
    Optional<Teacher> findByPhoneOrEmail(@Param("principal") String principal);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByTeacherId(String teacherId);

    List<Teacher> findByIsActiveTrue();

    List<Teacher> findAllByOrderByCreatedAtDesc();

    long countByIsVerifiedTrue();

    long countByIsVerifiedFalse();

    long countByIsPhoneVerifiedTrue();

    long countByIsPhoneVerifiedTrueAndIsVerifiedFalse();
}
