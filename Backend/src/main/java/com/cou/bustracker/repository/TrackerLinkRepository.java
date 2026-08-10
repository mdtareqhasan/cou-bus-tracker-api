package com.cou.bustracker.repository;

import com.cou.bustracker.entity.TrackerLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackerLinkRepository extends JpaRepository<TrackerLink, Long> {

    Optional<TrackerLink> findByBusId(Long busId);

    @Query("SELECT t FROM TrackerLink t JOIN FETCH t.bus WHERE t.bus.id = :busId")
    Optional<TrackerLink> findByBusIdWithBus(Long busId);
}
