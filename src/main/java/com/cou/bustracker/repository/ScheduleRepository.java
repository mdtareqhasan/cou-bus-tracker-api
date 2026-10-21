package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByIsActiveTrue();

    List<Schedule> findByBusIdAndIsActiveTrue(Long busId);

    long countByRouteId(Long routeId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus JOIN FETCH s.route WHERE s.isActive = true AND s.bus.isActive = true")
    List<Schedule> findAllWithBus();

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus JOIN FETCH s.route WHERE s.bus.id = :busId AND s.isActive = true AND s.bus.isActive = true")
    List<Schedule> findByBusIdWithBus(Long busId);

    /** Active schedules of one route — what the Route tab's detail page renders. */
    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus JOIN FETCH s.route WHERE s.route.id = :routeId AND s.isActive = true AND s.bus.isActive = true ORDER BY s.departureTime ASC")
    List<Schedule> findByRouteIdWithBus(Long routeId);

    /**
     * Active-schedule count per route, in one query for the route listing.
     * Buses count too: a schedule on a retired bus is invisible to riders
     * (see {@link #findAllWithBus}), so it must not inflate the badge.
     */
    @Query("SELECT s.route.id, count(s) FROM Schedule s JOIN s.bus b "
            + "WHERE s.isActive = true AND b.isActive = true GROUP BY s.route.id")
    Map<Long, Long> countActiveSchedulesByRoute();

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus JOIN FETCH s.route ORDER BY s.departureTime ASC")
    List<Schedule> findAllWithBusForAdmin();
}
