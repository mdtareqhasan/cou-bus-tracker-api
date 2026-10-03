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

    @NotNull(message = "বাস আইডি দিন")
    private Long busId;

    private String busName;

    @NotBlank(message = "প্রস্থান সময় দিন")
    private String departureTime;

    private String arrivalTime;

    @NotBlank(message = "দিক নির্দেশনা দিন")
    private String direction;

    private String startPoint;

    private String endPoint;

    private String days;
}
