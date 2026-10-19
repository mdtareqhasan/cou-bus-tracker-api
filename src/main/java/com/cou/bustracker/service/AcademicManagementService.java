package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.AcademicSessionRequest;
import com.cou.bustracker.dto.request.AcademicUnitRequest;
import com.cou.bustracker.dto.response.AcademicOptionResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-side CRUD for the academic reference data (departments, offices and
 * sessions) that used to be hardcoded in the Flutter app.
 *
 * <p>Two rules shape the write paths:
 *
 * <ul>
 *   <li><b>Deactivate, don't delete.</b> Existing students and teachers point
 *       at these rows by FK, so deleting a row that is still referenced would
 *       either fail on the constraint or orphan accounts. Deleting an in-use
 *       option is rejected with a conflict; {@code is_active = false} retires
 *       it from new registrations instead.</li>
 *   <li><b>Names are unique per kind.</b> Mirrors the DB constraints
 *       (units: type + Bengali name; sessions: either language form), but is
 *       checked in the service so the caller gets a stable error code rather
 *       than a raw constraint violation.</li>
 * </ul>
 *
 * <p>Unlike the public {@link AcademicReferenceService}, listings here include
 * retired rows — an admin must be able to see and re-activate what they turned
 * off.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AcademicManagementService {

    private final AcademicUnitRepository academicUnitRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    // ---- Listing -----------------------------------------------------------

    /** Every unit, active and retired, departments first. */
    public List<AcademicOptionResponse> getAllUnits() {
        return academicUnitRepository.findAllByOrderByUnitTypeAscSortOrderAsc()
                .stream()
                .map(AcademicManagementService::toResponse)
                .collect(Collectors.toList());
    }

    /** Every session, active and retired. */
    public List<AcademicOptionResponse> getAllSessions() {
        return academicSessionRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(AcademicManagementService::toResponse)
                .collect(Collectors.toList());
    }

    // ---- Units (departments / offices) -------------------------------------

    public AcademicOptionResponse createUnit(AcademicUnitRequest request) {
        String nameBn = request.getNameBn().trim();
        if (academicUnitRepository.existsByUnitTypeAndNameBn(request.getUnitType(), nameBn)) {
            throw new ApiException(ErrorCode.ACADEMIC_UNIT_NAME_EXISTS);
        }
        AcademicUnit unit = AcademicUnit.builder()
                .unitType(request.getUnitType())
                .nameBn(nameBn)
                .nameEn(request.getNameEn().trim())
                .sortOrder(request.getSortOrder())
                .isActive(true)
                .build();
        return toResponse(academicUnitRepository.save(unit));
    }

    public AcademicOptionResponse updateUnit(Long id, AcademicUnitRequest request) {
        AcademicUnit unit = findUnit(id);
        String nameBn = request.getNameBn().trim();
        if (academicUnitRepository.existsByUnitTypeAndNameBnAndIdNot(
                request.getUnitType(), nameBn, id)) {
            throw new ApiException(ErrorCode.ACADEMIC_UNIT_NAME_EXISTS);
        }
        unit.setUnitType(request.getUnitType());
        unit.setNameBn(nameBn);
        unit.setNameEn(request.getNameEn().trim());
        unit.setSortOrder(request.getSortOrder());
        return toResponse(academicUnitRepository.save(unit));
    }

    public void deleteUnit(Long id) {
        AcademicUnit unit = findUnit(id);
        long students = studentRepository.countByDepartmentId(id);
        long teachers = teacherRepository.countByDepartmentId(id);
        if (students > 0 || teachers > 0) {
            throw new ApiException(ErrorCode.ACADEMIC_UNIT_IN_USE,
                    "This option is assigned to " + students + " student(s) and "
                            + teachers + " teacher(s). Deactivate it instead of deleting");
        }
        academicUnitRepository.delete(unit);
    }

    /** Flips {@code is_active}; returns the new state for the success message. */
    public boolean toggleUnit(Long id) {
        AcademicUnit unit = findUnit(id);
        unit.setIsActive(!Boolean.TRUE.equals(unit.getIsActive()));
        academicUnitRepository.save(unit);
        return Boolean.TRUE.equals(unit.getIsActive());
    }

    // ---- Sessions ----------------------------------------------------------

    public AcademicOptionResponse createSession(AcademicSessionRequest request) {
        String nameBn = request.getNameBn().trim();
        String nameEn = request.getNameEn().trim();
        if (academicSessionRepository.existsByNameBn(nameBn)
                || academicSessionRepository.existsByNameEn(nameEn)) {
            throw new ApiException(ErrorCode.ACADEMIC_SESSION_NAME_EXISTS);
        }
        AcademicSession session = AcademicSession.builder()
                .nameBn(nameBn)
                .nameEn(nameEn)
                .sortOrder(request.getSortOrder())
                .isActive(true)
                .build();
        return toResponse(academicSessionRepository.save(session));
    }

    public AcademicOptionResponse updateSession(Long id, AcademicSessionRequest request) {
        AcademicSession session = findSession(id);
        String nameBn = request.getNameBn().trim();
        String nameEn = request.getNameEn().trim();
        if (academicSessionRepository.existsByNameBnAndIdNot(nameBn, id)
                || academicSessionRepository.existsByNameEnAndIdNot(nameEn, id)) {
            throw new ApiException(ErrorCode.ACADEMIC_SESSION_NAME_EXISTS);
        }
        session.setNameBn(nameBn);
        session.setNameEn(nameEn);
        session.setSortOrder(request.getSortOrder());
        return toResponse(academicSessionRepository.save(session));
    }

    public void deleteSession(Long id) {
        AcademicSession session = findSession(id);
        long students = studentRepository.countBySessionId(id);
        if (students > 0) {
            throw new ApiException(ErrorCode.ACADEMIC_SESSION_IN_USE,
                    "This session is assigned to " + students
                            + " student(s). Deactivate it instead of deleting");
        }
        academicSessionRepository.delete(session);
    }

    /** Flips {@code is_active}; returns the new state for the success message. */
    public boolean toggleSession(Long id) {
        AcademicSession session = findSession(id);
        session.setIsActive(!Boolean.TRUE.equals(session.getIsActive()));
        academicSessionRepository.save(session);
        return Boolean.TRUE.equals(session.getIsActive());
    }

    // ---- Lookup helpers ----------------------------------------------------

    private AcademicUnit findUnit(Long id) {
        return academicUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMIC_UNIT_NOT_FOUND));
    }

    private AcademicSession findSession(Long id) {
        return academicSessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMIC_SESSION_NOT_FOUND));
    }

    // ---- Mapping -----------------------------------------------------------

    private static AcademicOptionResponse toResponse(AcademicUnit unit) {
        return AcademicOptionResponse.builder()
                .id(unit.getId())
                // The Bengali name is the canonical string older clients stored.
                .value(unit.getNameBn())
                .labelBn(unit.getNameBn())
                .labelEn(unit.getNameEn())
                .type(unit.getUnitType().name())
                .sortOrder(unit.getSortOrder())
                .active(unit.getIsActive())
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
                .active(session.getIsActive())
                .build();
    }
}
