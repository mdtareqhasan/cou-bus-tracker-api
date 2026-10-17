package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.response.DashboardStatsResponse;
import com.cou.bustracker.repository.*;
import com.cou.bustracker.service.StudentService;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard stats (JWT required)")
public class AdminDashboardController {

    private final BusRepository busRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final NoticeRepository noticeRepository;
    private final ScheduleRepository scheduleRepository;

    @GetMapping
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        long totalBuses = busRepository.count();
        long activeBuses = busRepository.countByIsActiveTrue();

        // Counted with the same derived rule the listings and the app use
        // (department/roll/session filled in) rather than the stored flag, so
        // the numbers always agree with what the panel shows.
        long totalStudents = studentRepository.count();
        long verifiedStudents = studentRepository.findAll().stream()
                .filter(StudentService::isComplete)
                .count();
        long pendingStudents = totalStudents - verifiedStudents;

        long totalTeachers = teacherRepository.count();
        long verifiedTeachers = teacherRepository.findAll().stream()
                .filter(TeacherService::isComplete)
                .count();
        long pendingTeachers = totalTeachers - verifiedTeachers;

        long totalNotices = noticeRepository.count();
        long totalSchedules = scheduleRepository.count();

        return ResponseEntity.ok(DashboardStatsResponse.builder()
                .totalBuses(totalBuses)
                .activeBuses(activeBuses)
                .totalStudents(totalStudents)
                .verifiedStudents(verifiedStudents)
                .pendingStudents(pendingStudents)
                .totalTeachers(totalTeachers)
                .verifiedTeachers(verifiedTeachers)
                .pendingTeachers(pendingTeachers)
                .totalNotices(totalNotices)
                .totalSchedules(totalSchedules)
                .build());
    }
}
