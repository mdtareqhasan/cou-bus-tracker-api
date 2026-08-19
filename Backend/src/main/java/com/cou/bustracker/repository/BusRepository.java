package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    List<Bus> findByIsActiveTrue();

    long countByIsActiveTrue();

    List<Bus> findByCategoryAndIsActiveTrue(String category);

    Optional<Bus> findByBusNumber(String busNumber);

    @Query("SELECT b FROM Bus b LEFT JOIN FETCH b.trackerLink WHERE b.id = :id")
    Optional<Bus> findByIdWithTrackerLink(Long id);

    @Query("SELECT b FROM Bus b LEFT JOIN FETCH b.schedules WHERE b.id = :id")
    Optional<Bus> findByIdWithSchedules(Long id);

    @Query("SELECT b FROM Bus b LEFT JOIN FETCH b.trackerLink LEFT JOIN FETCH b.schedules WHERE b.id = :id")
    Optional<Bus> findByIdWithTrackerLinkAndSchedules(Long id);

    List<Bus> findAllByOrderByCreatedAtDesc();
}
