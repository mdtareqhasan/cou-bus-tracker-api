package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.response.DashboardStatsResponse;
import com.cou.bustracker.repository.*;
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

        long totalStudents = studentRepository.count();
        long verifiedStudents = studentRepository.countByIsVerifiedTrue();
        long pendingStudents = studentRepository.countByIsVerifiedFalse();

        long totalTeachers = teacherRepository.count();
        long verifiedTeachers = teacherRepository.countByIsVerifiedTrue();
        long pendingTeachers = teacherRepository.countByIsVerifiedFalse();

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
