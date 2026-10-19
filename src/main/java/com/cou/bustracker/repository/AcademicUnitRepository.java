package com.cou.bustracker.repository;

import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.AcademicUnit.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicUnitRepository extends JpaRepository<AcademicUnit, Long> {

    /** Active units of one kind, in the order the registration picker shows them. */
    List<AcademicUnit> findByUnitTypeAndIsActiveTrueOrderBySortOrderAsc(UnitType unitType);

    /** Both kinds, departments first — the teacher picker's order. */
    List<AcademicUnit> findByIsActiveTrueOrderByUnitTypeAscSortOrderAsc();

    /** Admin listing: every row, active and retired, departments first. */
    List<AcademicUnit> findAllByOrderByUnitTypeAscSortOrderAsc();

    boolean existsByUnitTypeAndNameBn(UnitType unitType, String nameBn);

    /** Uniqueness check for updates, ignoring the row being updated. */
    boolean existsByUnitTypeAndNameBnAndIdNot(UnitType unitType, String nameBn, Long id);
}
