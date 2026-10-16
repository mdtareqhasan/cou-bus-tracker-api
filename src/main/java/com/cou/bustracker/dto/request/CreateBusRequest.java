package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusRequest {

    @NotBlank(message = "বাস নম্বর দিন")
    private String busNumber;

    private String busName;

    @NotBlank(message = "বিভাগ/ক্যাটাগরি দিন")
    private String category;

    private String route;

    private String driverName;

    private String driverPhone;

    private String busImageUrl;
}
