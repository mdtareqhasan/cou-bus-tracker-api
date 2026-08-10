package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private Long id;
    private Long busId;
    private String busNumber;
    private String busName;
    private String category;
    private String departureTime;
    private String arrivalTime;
    private String direction;
    private String startPoint;
    private String endPoint;
    private String days;
}
