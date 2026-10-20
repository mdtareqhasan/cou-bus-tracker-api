package com.cou.bustracker.repository;

import com.cou.bustracker.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByIsActiveTrue();

    List<Route> findAllByOrderByIdAsc();

    boolean existsByNameBn(String nameBn);

    boolean existsByNameEn(String nameEn);

    boolean existsByNameBnAndIdNot(String nameBn, Long id);

    boolean existsByNameEnAndIdNot(String nameEn, Long id);
}
