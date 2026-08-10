package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalBuses;
    private long activeBuses;
    private long totalStudents;
    private long verifiedStudents;
    private long pendingStudents;
    private long totalTeachers;
    private long verifiedTeachers;
    private long pendingTeachers;
    private long totalNotices;
    private long totalSchedules;
}
