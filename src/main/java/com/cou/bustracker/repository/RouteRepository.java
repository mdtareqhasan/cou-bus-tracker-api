package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByIsActiveTrue();

    List<Route> findAllByOrderByIdAsc();

    /** The one route (if any) claiming {@code routeId} as its opposite — for healing stale links. */
    Optional<Route> findByOppositeRouteId(Long routeId);

    boolean existsByNameBn(String nameBn);

    boolean existsByNameEn(String nameEn);

    boolean existsByNameBnAndIdNot(String nameBn, Long id);

    boolean existsByNameEnAndIdNot(String nameEn, Long id);
}
