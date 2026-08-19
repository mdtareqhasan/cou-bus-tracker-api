package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByIsActiveTrue();

    List<Schedule> findByBusIdAndIsActiveTrue(Long busId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus WHERE s.isActive = true")
    List<Schedule> findAllWithBus();

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus WHERE s.bus.id = :busId AND s.isActive = true")
    List<Schedule> findByBusIdWithBus(Long busId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.bus ORDER BY s.departureTime ASC")
    List<Schedule> findAllWithBusForAdmin();
}
