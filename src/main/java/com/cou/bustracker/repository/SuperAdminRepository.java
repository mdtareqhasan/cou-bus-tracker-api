package com.cou.bustracker.repository;

import com.cou.bustracker.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    Optional<SuperAdmin> findByEmail(String email);

    boolean existsByEmail(String email);

    List<SuperAdmin> findByIsActiveTrue();
}
