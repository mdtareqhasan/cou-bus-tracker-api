package com.cou.bustracker.repository;

import com.cou.bustracker.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    Optional<AppConfig> findByConfigKey(String configKey);

    List<AppConfig> findAllByOrderByConfigKeyAsc();
}
