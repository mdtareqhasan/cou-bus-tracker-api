package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponse {

    private Long id;
    private String busNumber;
    private String busNameBn;
    private String busNameEn;
    private String category;
    private String driverName;
    private String driverPhone;
    private String busImageUrl;
    private String trackerUrl;
    private Boolean isActive;
}
