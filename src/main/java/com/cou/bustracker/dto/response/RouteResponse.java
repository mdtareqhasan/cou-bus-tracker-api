package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private String nameBn;
    private String nameEn;
    private Boolean isActive;

    /**
     * Id of the route running the opposite direction, or null when this is a
     * one-way route. The client pairs up rows by id — both rows are always in
     * the same listing response.
     */
    private Long oppositeRouteId;

    /**
     * Active schedules on this route. The Route tab uses it to label empty
     * routes ("no trips yet") without a second round trip.
     */
    private Long activeScheduleCount;
}
