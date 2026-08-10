package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequest {

    @NotNull(message = "Bus ID is required")
    private Long busId;

    private String busName;

    @NotBlank(message = "Departure time is required")
    private String departureTime;

    private String arrivalTime;

    @NotBlank(message = "Direction is required")
    private String direction;

    private String startPoint;

    private String endPoint;

    private String days;
}
