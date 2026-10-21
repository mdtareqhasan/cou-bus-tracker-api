package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RoutePreferenceRequest;
import com.cou.bustracker.dto.response.RoutePreferenceResponse;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.UserRoutePreference;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.UserRoutePreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The rider's own configuration of each route: pinned on the home screen,
 * and/or opted in to schedule-change pushes.
 *
 * <p>Two rules shape the write path:
 *
 * <ul>
 *   <li><b>Two independent flags, one row.</b> Pinning and alerting are
 *       separate choices (a rider may quietly follow a route they don't
 *       commute on). A row where both flags are false is deleted rather than
 *       stored — it would carry no information and would still show up in the
 *       dispatcher's audience scan.</li>
 *   <li><b>Only riders have preferences.</b> The schema CHECK allows STUDENT
 *       and TEACHER; an admin/super-admin caller gets a clear 400 instead of
 *       a raw Postgres violation — same contract as FCM token registration.
 *   </li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserRoutePreferenceService {

    /** Roles the schema allows in user_route_preferences.user_type. */
    private static final Set<String> SUPPORTED_ROLES = Set.of("STUDENT", "TEACHER");

    private final UserRoutePreferenceRepository preferenceRepository;
    private final RouteRepository routeRepository;

    /** Every saved preference of the caller, active routes only. */
    @Transactional(readOnly = true)
    public List<RoutePreferenceResponse> getMyPreferences(CurrentUser user) {
        requireRider(user);
        return preferenceRepository.findByUserTypeAndUserId(user.role(), user.userId()).stream()
                .map(UserRoutePreferenceService::toResponse)
                .toList();
    }

    /**
     * Idempotent upsert of one route's flags. Null fields keep their current
     * value; a result of (false, false) deletes the row.
     */
    public RoutePreferenceResponse upsertPreference(CurrentUser user, Long routeId,
                                                    RoutePreferenceRequest request) {
        requireRider(user);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROUTE_NOT_FOUND));

        Optional<UserRoutePreference> existing = preferenceRepository
                .findByUserTypeAndUserIdAndRouteId(user.role(), user.userId(), routeId);
        UserRoutePreference preference = existing.orElseGet(() -> UserRoutePreference.builder()
                .userType(user.role())
                .userId(user.userId())
                .routeId(route.getId())
                .isPinned(false)
                .scheduleAlertsEnabled(false)
                .build());

        preference.setIsPinned(valueOr(request.getIsPinned(), preference.getIsPinned()));
        preference.setScheduleAlertsEnabled(
                valueOr(request.getScheduleAlertsEnabled(), preference.getScheduleAlertsEnabled()));

        if (!Boolean.TRUE.equals(preference.getIsPinned())
                && !Boolean.TRUE.equals(preference.getScheduleAlertsEnabled())) {
            // Only touch the repository when a row actually exists: deleting a
            // just-built (never saved) entity would make Spring Data merge it
            // — inserting a row only to remove it.
            if (existing.isPresent()) {
                preferenceRepository.delete(preference);
                log.info("Cleared both route flags for {} {} on route {} — preference row removed",
                        user.role(), user.userId(), routeId);
            }
            return toResponse(preference);
        }

        return toResponse(preferenceRepository.save(preference));
    }

    /** Idempotent: forgetting a route that was never saved is not an error. */
    public void removePreference(CurrentUser user, Long routeId) {
        requireRider(user);
        preferenceRepository.deleteByUserTypeAndUserIdAndRouteId(user.role(), user.userId(), routeId);
    }

    /**
     * The push audience for one route: every (user_type, user_id) with
     * {@code schedule_alerts_enabled} on. Called by the notification dispatcher
     * on each schedule change, hence the covering index
     * {@code idx_user_route_pref_route_alerts} from the V3 migration.
     *
     * <p>Empty map when nobody follows the route — a change to a route nobody
     * opted into is not news, and must never widen to a broadcast.
     */
    @Transactional(readOnly = true)
    public Map<String, Set<Long>> alertSubscribersByType(Long routeId) {
        Map<String, Set<Long>> byType = new LinkedHashMap<>();
        for (UserRoutePreference preference
                : preferenceRepository.findByRouteIdAndScheduleAlertsEnabledTrue(routeId)) {
            byType.computeIfAbsent(preference.getUserType(), k -> new LinkedHashSet<>())
                    .add(preference.getUserId());
        }
        return byType;
    }

    private static void requireRider(CurrentUser user) {
        if (user == null || !SUPPORTED_ROLES.contains(user.role())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED,
                    "Route preferences are only available to students and teachers");
        }
    }

    private static Boolean valueOr(Boolean requested, Boolean current) {
        return requested != null ? requested : Boolean.TRUE.equals(current);
    }

    private static RoutePreferenceResponse toResponse(UserRoutePreference preference) {
        return RoutePreferenceResponse.builder()
                .routeId(preference.getRouteId())
                .isPinned(preference.getIsPinned())
                .scheduleAlertsEnabled(preference.getScheduleAlertsEnabled())
                .build();
    }
}
