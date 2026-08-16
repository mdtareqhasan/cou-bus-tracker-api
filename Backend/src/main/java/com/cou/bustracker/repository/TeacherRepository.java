package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByTeacherId(String teacherId);

    List<Teacher> findByIsActiveTrue();

    List<Teacher> findAllByOrderByCreatedAtDesc();

    long countByIsVerifiedTrue();

    long countByIsVerifiedFalse();
}
