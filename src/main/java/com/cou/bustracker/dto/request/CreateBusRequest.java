package com.cou.bustracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusRequest {

    @NotBlank(message = "Bus number is required")
    private String busNumber;

    private String busNameBn;

    private String busNameEn;

    @NotBlank(message = "Department/category is required")
    private String category;

    private String driverName;

    private String driverPhone;

    private String busImageUrl;
}
