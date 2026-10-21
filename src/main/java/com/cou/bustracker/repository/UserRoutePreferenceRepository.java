package com.cou.bustracker.repository;

import com.cou.bustracker.entity.UserRoutePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoutePreferenceRepository extends JpaRepository<UserRoutePreference, Long> {

    /** Everything one rider has saved, for the Route tab and the pin sheet. */
    List<UserRoutePreference> findByUserTypeAndUserId(String userType, Long userId);

    Optional<UserRoutePreference> findByUserTypeAndUserIdAndRouteId(String userType, Long userId, Long routeId);

    /**
     * The push audience for one route. Called by the dispatcher on every
     * schedule change, hence the covering index
     * {@code idx_user_route_pref_route_alerts} in the V3 migration.
     */
    List<UserRoutePreference> findByRouteIdAndScheduleAlertsEnabledTrue(Long routeId);

    void deleteByUserTypeAndUserIdAndRouteId(String userType, Long userId, Long routeId);
}
