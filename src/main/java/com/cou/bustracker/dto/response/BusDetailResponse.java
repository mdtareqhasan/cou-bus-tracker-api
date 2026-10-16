package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusDetailResponse {

    private Long id;
    private String busNumber;
    private String busName;
    private String category;
    private String route;
    private String driverName;
    private String driverPhone;
    private String busImageUrl;
    private String trackerUrl;
    private Boolean isActive;
    private List<ScheduleResponse> schedules;
}
