package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    List<Student> findByIsActiveTrue();

    List<Student> findAllByOrderByCreatedAtDesc();

    long countByIsVerifiedTrue();

    long countByIsVerifiedFalse();

    long countByIsEmailVerifiedTrue();

    long countByIsEmailVerifiedTrueAndIsVerifiedFalse();
}
