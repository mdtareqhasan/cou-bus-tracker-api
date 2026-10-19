package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.AcademicSessionRequest;
import com.cou.bustracker.dto.request.AcademicUnitRequest;
import com.cou.bustracker.dto.response.AcademicOptionResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.AcademicUnit.UnitType;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicManagementServiceTest {

    @Mock
    private AcademicUnitRepository academicUnitRepository;

    @Mock
    private AcademicSessionRepository academicSessionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private AcademicManagementService academicManagementService;

    private AcademicUnit unit(Long id, UnitType type, String nameBn, boolean active) {
        return AcademicUnit.builder()
                .id(id)
                .unitType(type)
                .nameBn(nameBn)
                .nameEn("English name")
                .sortOrder(1)
                .isActive(active)
                .build();
    }

    private AcademicSession session(Long id, String nameBn, String nameEn) {
        return AcademicSession.builder()
                .id(id)
                .nameBn(nameBn)
                .nameEn(nameEn)
                .sortOrder(1)
                .isActive(true)
                .build();
    }

    // ---- Units -------------------------------------------------------------

    @Test
    void createUnit_shouldRejectDuplicateNameWithinType() {
        AcademicUnitRequest request = new AcademicUnitRequest(
                UnitType.DEPARTMENT, "  গণিত  ", "Mathematics", 2);

        when(academicUnitRepository.existsByUnitTypeAndNameBn(UnitType.DEPARTMENT, "গণিত"))
                .thenReturn(true);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.createUnit(request));

        assertEquals(ErrorCode.ACADEMIC_UNIT_NAME_EXISTS, ex.getErrorCode());
        verify(academicUnitRepository, never()).save(any(AcademicUnit.class));
    }

    @Test
    void createUnit_shouldTrimAndSaveNewOption() {
        AcademicUnitRequest request = new AcademicUnitRequest(
                UnitType.OFFICE, "  আইসিটি সেল  ", "ICT Cell", 16);

        when(academicUnitRepository.existsByUnitTypeAndNameBn(UnitType.OFFICE, "আইসিটি সেল"))
                .thenReturn(false);
        when(academicUnitRepository.save(any(AcademicUnit.class)))
                .thenAnswer(invocation -> {
                    AcademicUnit saved = invocation.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });

        AcademicOptionResponse response = academicManagementService.createUnit(request);

        ArgumentCaptor<AcademicUnit> captor = ArgumentCaptor.forClass(AcademicUnit.class);
        verify(academicUnitRepository).save(captor.capture());
        assertEquals("আইসিটি সেল", captor.getValue().getNameBn());
        assertEquals(UnitType.OFFICE, captor.getValue().getUnitType());
        assertEquals(99L, response.getId());
        assertTrue(response.getActive());
    }

    @Test
    void deleteUnit_shouldRejectWhenStudentsOrTeachersUseIt() {
        when(academicUnitRepository.findById(5L))
                .thenReturn(Optional.of(unit(5L, UnitType.DEPARTMENT, "গণিত", true)));
        when(studentRepository.countByDepartmentId(5L)).thenReturn(3L);
        when(teacherRepository.countByDepartmentId(5L)).thenReturn(1L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.deleteUnit(5L));

        assertEquals(ErrorCode.ACADEMIC_UNIT_IN_USE, ex.getErrorCode());
        verify(academicUnitRepository, never()).delete(any(AcademicUnit.class));
    }

    @Test
    void deleteUnit_shouldDeleteWhenUnreferenced() {
        AcademicUnit referenced = unit(7L, UnitType.OFFICE, "কেন্দ্রীয় মসজিদ", true);
        when(academicUnitRepository.findById(7L)).thenReturn(Optional.of(referenced));
        when(studentRepository.countByDepartmentId(7L)).thenReturn(0L);
        when(teacherRepository.countByDepartmentId(7L)).thenReturn(0L);

        academicManagementService.deleteUnit(7L);

        verify(academicUnitRepository).delete(referenced);
    }

    @Test
    void toggleUnit_shouldFlipActiveState() {
        AcademicUnit inactive = unit(3L, UnitType.DEPARTMENT, "গণিত", false);
        when(academicUnitRepository.findById(3L)).thenReturn(Optional.of(inactive));
        when(academicUnitRepository.save(any(AcademicUnit.class))).thenReturn(inactive);

        assertTrue(academicManagementService.toggleUnit(3L));
        assertTrue(inactive.getIsActive());
    }

    @Test
    void updateUnit_shouldAllowRenamingToItsOwnName_ButRejectAnotherRowsName() {
        AcademicUnit existing = unit(10L, UnitType.DEPARTMENT, "গণিত", true);
        when(academicUnitRepository.findById(10L)).thenReturn(Optional.of(existing));
        // The duplicate belongs to row 11, so row 10 renaming to itself must pass.
        when(academicUnitRepository.existsByUnitTypeAndNameBnAndIdNot(
                UnitType.DEPARTMENT, "গণিত", 10L)).thenReturn(false);
        when(academicUnitRepository.save(existing)).thenReturn(existing);

        AcademicUnitRequest request = new AcademicUnitRequest(
                UnitType.DEPARTMENT, "গণিত", "Mathematics", 2);
        AcademicOptionResponse response = academicManagementService.updateUnit(10L, request);

        assertEquals(10L, response.getId());
        verify(academicUnitRepository).save(existing);

        when(academicUnitRepository.existsByUnitTypeAndNameBnAndIdNot(
                UnitType.DEPARTMENT, "রসায়ন", 10L)).thenReturn(true);
        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.updateUnit(10L,
                        new AcademicUnitRequest(UnitType.DEPARTMENT, "রসায়ন", "Chemistry", 5)));
        assertEquals(ErrorCode.ACADEMIC_UNIT_NAME_EXISTS, ex.getErrorCode());
    }

    // ---- Sessions ------------------------------------------------------------

    @Test
    void createSession_shouldRejectDuplicateEitherName() {
        AcademicSessionRequest request = new AcademicSessionRequest("২০২০-২০২১", "2020-2021", 5);

        when(academicSessionRepository.existsByNameBn("২০২০-২০২১")).thenReturn(false);
        when(academicSessionRepository.existsByNameEn("2020-2021")).thenReturn(true);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.createSession(request));

        assertEquals(ErrorCode.ACADEMIC_SESSION_NAME_EXISTS, ex.getErrorCode());
        verify(academicSessionRepository, never()).save(any(AcademicSession.class));
    }

    @Test
    void deleteSession_shouldRejectWhenStudentsUseIt() {
        when(academicSessionRepository.findById(4L))
                .thenReturn(Optional.of(session(4L, "২০১৯-২০২০", "2019-2020")));
        when(studentRepository.countBySessionId(4L)).thenReturn(12L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.deleteSession(4L));

        assertEquals(ErrorCode.ACADEMIC_SESSION_IN_USE, ex.getErrorCode());
        verify(academicSessionRepository, never()).delete(any(AcademicSession.class));
    }

    @Test
    void deleteSession_shouldDeleteWhenNoStudentReferencesIt() {
        AcademicSession free = session(8L, "২০১৬-২০১৭", "2016-2017");
        when(academicSessionRepository.findById(8L)).thenReturn(Optional.of(free));
        when(studentRepository.countBySessionId(8L)).thenReturn(0L);

        academicManagementService.deleteSession(8L);

        verify(academicSessionRepository).delete(free);
    }

    @Test
    void toggleSession_shouldFlipActiveState() {
        AcademicSession active = session(2L, "২০১৭-২০১৮", "2017-2018");
        active.setIsActive(true);
        when(academicSessionRepository.findById(2L)).thenReturn(Optional.of(active));
        when(academicSessionRepository.save(any(AcademicSession.class))).thenReturn(active);

        assertFalse(academicManagementService.toggleSession(2L));
        assertFalse(active.getIsActive());
    }

    @Test
    void deleteUnit_shouldReportNotFoundForUnknownId() {
        when(academicUnitRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(
                ApiException.class,
                () -> academicManagementService.deleteUnit(404L));

        assertEquals(ErrorCode.ACADEMIC_UNIT_NOT_FOUND, ex.getErrorCode());
    }
}
