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
    private String busNameBn;
    private String busNameEn;
    private String category;
    private String departureTime;
    private String arrivalTime;
    private String direction;
    private Long routeId;
    private String routeNameBn;
    private String routeNameEn;
    private String days;
    private Boolean isActive;
}
