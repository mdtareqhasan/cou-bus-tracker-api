package com.cou.bustracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    /**
     * The same schedules broken down by bus category, e.g.
     * {@code {TEACHER=2, BLUE=1}}. The listing is public and role-less, so the
     * role gate cannot be applied server-side: the client sums only the
     * categories its signed-in rider may ride. Without this a student is told
     * a teacher-only route has trips.
     */
    private Map<String, Long> activeScheduleCountsByCategory;
}
