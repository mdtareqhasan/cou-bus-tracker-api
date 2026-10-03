package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.EmailVerificationInitRequest;
import com.cou.bustracker.entity.EmailVerificationOtp.UserRole;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailVerificationServiceTest {

    private EmailVerificationService service;

    private void newService() {
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail", "existsByRollNumber" -> false;
                    case "save" -> args[0];
                    default -> null;
                });
        TeacherRepository teacherRepository = (TeacherRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TeacherRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail", "existsByTeacherId" -> false;
                    case "save" -> args[0];
                    default -> null;
                });
        service = new EmailVerificationService(studentRepository, teacherRepository, null, null);
    }

    private EmailVerificationInitRequest studentRequest(String email) {
        EmailVerificationInitRequest request = new EmailVerificationInitRequest();
        request.setRole(UserRole.STUDENT);
        request.setName("Test Student");
        request.setEmail(email);
        request.setPassword("secret123");
        request.setDepartment("CSE");
        request.setRollNumber("1607041");
        request.setSession("2016-17");
        return request;
    }

    private EmailVerificationInitRequest employeeRequest(String email) {
        EmailVerificationInitRequest request = new EmailVerificationInitRequest();
        request.setRole(UserRole.EMPLOYEE);
        request.setName("Test Employee");
        request.setEmail(email);
        request.setPassword("secret123");
        request.setDepartment("Admin");
        request.setEmployeeId("EMP-1001");
        request.setDesignation("Officer");
        return request;
    }

    @Test
    void initRegistration_allowsStudentEmailAtStudCouDomain() {
        newService();
        assertDoesNotThrow(() -> service.initRegistration(studentRequest("student@stud.cou.ac.bd")));
    }

    @Test
    void initRegistration_allowsEmployeeEmailAtCouDomain() {
        newService();
        assertDoesNotThrow(() -> service.initRegistration(employeeRequest("teacher@cou.ac.bd")));
    }

    @Test
    void initRegistration_rejectsOtherDomainForStudent() {
        newService();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.initRegistration(studentRequest("student@gmail.com")));
        assertTrue(ex.getMessage().contains("@stud.cou.ac.bd or @cou.ac.bd"));
    }

    @Test
    void initRegistration_rejectsOtherDomainForEmployee() {
        newService();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.initRegistration(employeeRequest("employee@yahoo.com")));
        assertTrue(ex.getMessage().contains("@stud.cou.ac.bd or @cou.ac.bd"));
    }
}
