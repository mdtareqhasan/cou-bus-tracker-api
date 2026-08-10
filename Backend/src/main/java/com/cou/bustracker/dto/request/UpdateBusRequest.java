package com.cou.bustracker.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusRequest {

    private String busNumber;
    private String busName;
    private String category;
    private String route;
    private String driverName;
    private String driverPhone;
    private String busImageUrl;
    private Boolean isActive;
}
