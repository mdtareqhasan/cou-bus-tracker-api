package com.cou.bustracker.repository;

import com.cou.bustracker.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {

    /** Active sessions in the order the registration picker shows them. */
    List<AcademicSession> findByIsActiveTrueOrderBySortOrderAsc();

    /** Admin listing: every row, active and retired. */
    List<AcademicSession> findAllByOrderBySortOrderAsc();

    boolean existsByNameBn(String nameBn);

    boolean existsByNameEn(String nameEn);

    /** Uniqueness checks for updates, ignoring the row being updated. */
    boolean existsByNameBnAndIdNot(String nameBn, Long id);

    boolean existsByNameEnAndIdNot(String nameEn, Long id);
}
