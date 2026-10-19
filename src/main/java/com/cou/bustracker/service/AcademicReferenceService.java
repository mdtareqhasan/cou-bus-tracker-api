package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.AcademicOptionResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.AcademicUnit.UnitType;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only access to the academic reference data that used to be hardcoded
 * in the Flutter app (departments/offices and sessions).
 *
 * <p>Both language forms are returned for every option so the client renders
 * whichever matches its locale — the server deliberately does not pick a
 * language. Only {@code is_active} rows are exposed; retiring an option
 * hides it from new registrations without orphaning existing accounts.
 */
@Service
@RequiredArgsConstructor
public class AcademicReferenceService {

    private final AcademicUnitRepository academicUnitRepository;
    private final AcademicSessionRepository academicSessionRepository;

    /** Teaching departments — what the student picker offers. */
    public List<AcademicOptionResponse> getDepartments() {
        return mapUnits(academicUnitRepository
                .findByUnitTypeAndIsActiveTrueOrderBySortOrderAsc(UnitType.DEPARTMENT));
    }

    /** Administrative offices — teacher-only, shown after the departments. */
    public List<AcademicOptionResponse> getOffices() {
        return mapUnits(academicUnitRepository
                .findByUnitTypeAndIsActiveTrueOrderBySortOrderAsc(UnitType.OFFICE));
    }

    /** Departments then offices — the teacher picker's combined order. */
    public List<AcademicOptionResponse> getAllUnits() {
        return mapUnits(academicUnitRepository
                .findByIsActiveTrueOrderByUnitTypeAscSortOrderAsc());
    }

    public List<AcademicOptionResponse> getSessions() {
        return academicSessionRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(AcademicReferenceService::toResponse)
                .collect(Collectors.toList());
    }

    private static List<AcademicOptionResponse> mapUnits(List<AcademicUnit> units) {
        return units.stream()
                .map(AcademicReferenceService::toResponse)
                .collect(Collectors.toList());
    }

    private static AcademicOptionResponse toResponse(AcademicUnit unit) {
        return AcademicOptionResponse.builder()
                .id(unit.getId())
                // The Bengali name is the canonical string older clients stored.
                .value(unit.getNameBn())
                .labelBn(unit.getNameBn())
                .labelEn(unit.getNameEn())
                .type(unit.getUnitType().name())
                .sortOrder(unit.getSortOrder())
                // Only active rows are listed here, so this is always true.
                .active(true)
                .build();
    }

    private static AcademicOptionResponse toResponse(AcademicSession session) {
        return AcademicOptionResponse.builder()
                .id(session.getId())
                // The English range is the canonical string older clients stored.
                .value(session.getNameEn())
                .labelBn(session.getNameBn())
                .labelEn(session.getNameEn())
                .sortOrder(session.getSortOrder())
                // Only active rows are listed here, so this is always true.
                .active(true)
                .build();
    }
}
