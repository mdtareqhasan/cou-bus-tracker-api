package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.AcademicSession;
import com.cou.bustracker.entity.AcademicUnit;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.AcademicSessionRepository;
import com.cou.bustracker.repository.AcademicUnitRepository;
import com.cou.bustracker.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Google-created student row has no department/roll/session. It must be
 * possible to fill them exactly once — and never to swap them afterwards,
 * because a roll number may only ever belong to one account.
 *
 * <p>Department and session are now referenced by row id; the service
 * resolves them and rejects anything that isn't an active DEPARTMENT unit /
 * active session.
 */
public class StudentServiceAcademicDetailsTest {

    private static final Long DEPT_ID = 3L;
    private static final Long SESSION_ID = 9L;

    private StudentRepository studentRepository;
    private AcademicUnitRepository academicUnitRepository;
    private AcademicSessionRepository academicSessionRepository;
    private StudentService studentService;
    private Student student;
    private AcademicUnit mathematics;
    private AcademicSession session2019;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        academicUnitRepository = mock(AcademicUnitRepository.class);
        academicSessionRepository = mock(AcademicSessionRepository.class);
        studentService = new StudentService(studentRepository, null, academicUnitRepository,
                academicSessionRepository, null, null, null, null);

        mathematics = AcademicUnit.builder()
                .id(DEPT_ID)
                .unitType(AcademicUnit.UnitType.DEPARTMENT)
                .nameBn("গণিত")
                .nameEn("Mathematics")
                .isActive(true)
                .build();
        session2019 = AcademicSession.builder()
                .id(SESSION_ID)
                .nameBn("২০১৯-২০২০")
                .nameEn("2019-2020")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(DEPT_ID)).thenReturn(Optional.of(mathematics));
        when(academicSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session2019));

        student = Student.builder()
                .id(7L)
                .name("Student")
                .email("student@stud.cou.ac.bd")
                .isVerified(false)
                .isActive(true)
                .build();
        when(studentRepository.findByEmail("student@stud.cou.ac.bd"))
                .thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StudentAcademicDetailsRequest request(Long departmentId, String roll, Long sessionId) {
        return new StudentAcademicDetailsRequest(departmentId, roll, sessionId);
    }

    @Test
    void googleCreatedStudent_canFillAcademicDetailsOnce() {
        StudentResponse response = studentService.updateAcademicDetails(
                "student@stud.cou.ac.bd",
                request(DEPT_ID, "190345", SESSION_ID));

        assertEquals(DEPT_ID, response.getDepartmentId());
        assertEquals(SESSION_ID, response.getSessionId());
        assertEquals("গণিত", response.getDepartment());
        assertEquals("190345", response.getRollNumber());
        assertEquals("২০১৯-২০২০", response.getSession());
        assertEquals(mathematics, student.getDepartment());
        assertEquals("190345", student.getRollNumber());
        assertEquals(session2019, student.getSession());
        verify(studentRepository).save(student);
    }

    @Test
    void officeUnit_isRejectedForStudent() {
        AcademicUnit office = AcademicUnit.builder()
                .id(30L)
                .unitType(AcademicUnit.UnitType.OFFICE)
                .nameBn("রেজিস্ট্রারের কার্যালয়")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(30L)).thenReturn(Optional.of(office));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(30L, "190345", SESSION_ID)));

        assertEquals(ErrorCode.DEPARTMENT_REQUIRED, exception.getErrorCode());
        verify(studentRepository, never()).save(any(Student.class));
        assertNull(student.getDepartment());
    }

    @Test
    void rollNumberOwnedByAnotherAccount_isRejected() {
        when(studentRepository.existsByRollNumber("190345")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(DEPT_ID, "190345", SESSION_ID)));

        assertEquals(ErrorCode.ROLL_ALREADY_REGISTERED, exception.getErrorCode());
        verify(studentRepository, never()).save(any(Student.class));
        assertNull(student.getRollNumber());
    }

    @Test
    void populatedRollNumber_cannotBeChanged() {
        student.setDepartment(mathematics);
        student.setRollNumber("190345");
        student.setSession(session2019);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(DEPT_ID, "200001", SESSION_ID)));

        assertEquals(ErrorCode.ROLL_NUMBER_IMMUTABLE, exception.getErrorCode());
        assertEquals("190345", student.getRollNumber());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void populatedDepartment_cannotBeSwapped() {
        student.setDepartment(mathematics);
        student.setRollNumber("190345");
        student.setSession(session2019);

        AcademicUnit physics = AcademicUnit.builder()
                .id(4L)
                .unitType(AcademicUnit.UnitType.DEPARTMENT)
                .nameBn("পদার্থবিজ্ঞান")
                .isActive(true)
                .build();
        when(academicUnitRepository.findById(4L)).thenReturn(Optional.of(physics));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(4L, "190345", SESSION_ID)));

        assertEquals(ErrorCode.DEPARTMENT_IMMUTABLE, exception.getErrorCode());
        assertEquals(mathematics, student.getDepartment());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void identicalDetails_forAlreadyCompleteProfile_areAcceptedWithoutWrite() {
        student.setDepartment(mathematics);
        student.setRollNumber("190345");
        student.setSession(session2019);

        StudentResponse response = studentService.updateAcademicDetails(
                "student@stud.cou.ac.bd",
                request(DEPT_ID, "190345", SESSION_ID));

        assertEquals("190345", response.getRollNumber());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void missingValue_isRejected() {
        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(null, "190345", SESSION_ID)));

        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(DEPT_ID, "  ", SESSION_ID)));

        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(DEPT_ID, "190345", null)));
    }
}
