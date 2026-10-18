package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Google-created student row has no department/roll/session. It must be
 * possible to fill them exactly once — and never to swap them afterwards,
 * because a roll number may only ever belong to one account.
 */
public class StudentServiceAcademicDetailsTest {

    private StudentRepository studentRepository;
    private StudentService studentService;
    private Student student;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        studentService = new StudentService(studentRepository, null, null, null, null, null);
        student = Student.builder()
                .id(7L)
                .name("Student")
                .email("student@stud.cou.ac.bd")
                .isVerified(false)
                .isActive(true)
                .build();
        when(studentRepository.findByPhoneOrEmail("student@stud.cou.ac.bd"))
                .thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StudentAcademicDetailsRequest request(String department, String roll, String session) {
        return new StudentAcademicDetailsRequest(department, roll, session);
    }

    @Test
    void googleCreatedStudent_canFillAcademicDetailsOnce() {
        StudentResponse response = studentService.updateAcademicDetails(
                "student@stud.cou.ac.bd",
                request("গণিত", "190345", "2019-2020"));

        assertEquals("গণিত", response.getDepartment());
        assertEquals("190345", response.getRollNumber());
        assertEquals("2019-2020", response.getSession());
        assertEquals("গণিত", student.getDepartment());
        assertEquals("190345", student.getRollNumber());
        assertEquals("2019-2020", student.getSession());
        verify(studentRepository).save(student);
    }

    @Test
    void rollNumberOwnedByAnotherAccount_isRejected() {
        when(studentRepository.existsByRollNumber("190345")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request("গণিত", "190345", "2019-2020")));

        assertEquals(ErrorCode.ROLL_ALREADY_REGISTERED, exception.getErrorCode());
        verify(studentRepository, never()).save(any(Student.class));
        assertNull(student.getRollNumber());
    }

    @Test
    void populatedRollNumber_cannotBeChanged() {
        student.setDepartment("গণিত");
        student.setRollNumber("190345");
        student.setSession("2019-2020");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request("গণিত", "200001", "2019-2020")));

        assertEquals(ErrorCode.ROLL_NUMBER_IMMUTABLE, exception.getErrorCode());
        assertEquals("190345", student.getRollNumber());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void identicalDetails_forAlreadyCompleteProfile_areAcceptedWithoutWrite() {
        student.setDepartment("গণিত");
        student.setRollNumber("190345");
        student.setSession("2019-2020");

        StudentResponse response = studentService.updateAcademicDetails(
                "student@stud.cou.ac.bd",
                request("গণিত", "190345", "2019-2020"));

        assertEquals("190345", response.getRollNumber());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void missingValue_isRejected() {
        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request(null, "190345", "2019-2020")));

        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request("গণিত", "  ", "2019-2020")));

        assertThrows(ApiException.class,
                () -> studentService.updateAcademicDetails(
                        "student@stud.cou.ac.bd", request("গণিত", "190345", null)));
    }
}
