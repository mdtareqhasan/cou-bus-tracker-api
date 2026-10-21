package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.RoutePreferenceRequest;
import com.cou.bustracker.dto.response.RoutePreferenceResponse;
import com.cou.bustracker.entity.Route;
import com.cou.bustracker.entity.UserRoutePreference;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.cou.bustracker.repository.RouteRepository;
import com.cou.bustracker.repository.UserRoutePreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins down the rules behind {@code user_route_preferences}: null-means-keep,
 * both-false-means-delete, riders-only, and the audience grouping the push
 * dispatcher leans on. A saved row is the sole targeting signal for schedule
 * notifications, so a bug here silently mis-routes pushes.
 */
class UserRoutePreferenceServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(10L, "STUDENT", "student@couniv.edu");
    private static final CurrentUser ADMIN = new CurrentUser(1L, "ADMIN", "admin@couniv.edu");

    private UserRoutePreferenceRepository preferenceRepository;
    private RouteRepository routeRepository;
    private UserRoutePreferenceService service;

    @BeforeEach
    void setUp() {
        preferenceRepository = mock(UserRoutePreferenceRepository.class);
        routeRepository = mock(RouteRepository.class);
        service = new UserRoutePreferenceService(preferenceRepository, routeRepository);

        when(routeRepository.findById(4L)).thenReturn(Optional.of(
                Route.builder().id(4L).nameBn("টেস্ট").nameEn("Test").isActive(true).build()));
        when(preferenceRepository.save(any(UserRoutePreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- Upsert rules ----------------------------------------------------

    @Test
    void firstPinCreatesRowWithOnlyThatFlagOn() {
        when(preferenceRepository.findByUserTypeAndUserIdAndRouteId("STUDENT", 10L, 4L))
                .thenReturn(Optional.empty());

        RoutePreferenceResponse response =
                service.upsertPreference(STUDENT, 4L, new RoutePreferenceRequest(true, null));

        assertTrue(response.getIsPinned());
        assertFalse(response.getScheduleAlertsEnabled(),
                "the pin must not silently opt the rider into pushes");
        verify(preferenceRepository).save(any(UserRoutePreference.class));
    }

    @Test
    void nullFieldKeepsTheCurrentValue() {
        UserRoutePreference existing = preference(true, false);
        when(preferenceRepository.findByUserTypeAndUserIdAndRouteId("STUDENT", 10L, 4L))
                .thenReturn(Optional.of(existing));

        // Flipping just the bell must not echo back (and so keep) the pin —
        // the request deliberately omits it.
        RoutePreferenceResponse response =
                service.upsertPreference(STUDENT, 4L, new RoutePreferenceRequest(null, true));

        assertTrue(response.getIsPinned());
        assertTrue(response.getScheduleAlertsEnabled());
    }

    @Test
    void clearingBothFlagsDeletesTheRowInsteadOfStoringIt() {
        UserRoutePreference existing = preference(true, true);
        when(preferenceRepository.findByUserTypeAndUserIdAndRouteId("STUDENT", 10L, 4L))
                .thenReturn(Optional.of(existing));

        RoutePreferenceResponse response =
                service.upsertPreference(STUDENT, 4L, new RoutePreferenceRequest(false, false));

        assertFalse(response.getIsPinned());
        assertFalse(response.getScheduleAlertsEnabled());
        verify(preferenceRepository).delete(existing);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void aRequestThatWouldLeaveBothFlagsFalseOnNewRowNeverCreatesOne() {
        when(preferenceRepository.findByUserTypeAndUserIdAndRouteId("STUDENT", 10L, 4L))
                .thenReturn(Optional.empty());

        RoutePreferenceResponse response =
                service.upsertPreference(STUDENT, 4L, new RoutePreferenceRequest(false, false));

        assertFalse(response.getIsPinned());
        assertFalse(response.getScheduleAlertsEnabled());
        verify(preferenceRepository, never()).save(any());
        verify(preferenceRepository, never()).delete(any());
    }

    // --- Guards ----------------------------------------------------------

    @Test
    void adminsCannotSaveRoutePreferences() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsertPreference(ADMIN, 4L, new RoutePreferenceRequest(true, null)));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void unknownRouteIsRejected() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsertPreference(STUDENT, 99L, new RoutePreferenceRequest(true, null)));

        assertEquals(ErrorCode.ROUTE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void forgettingAnUnsavedRouteIsNotAnError() {
        // The app clears both flags on the server before dropping a route;
        // calling delete again must be a no-op, not a 404.
        assertDoesNotThrow(() -> service.removePreference(STUDENT, 4L));

        verify(preferenceRepository).deleteByUserTypeAndUserIdAndRouteId("STUDENT", 10L, 4L);
    }

    @Test
    void adminsCannotListRoutePreferences() {
        ApiException ex = assertThrows(ApiException.class, () -> service.getMyPreferences(ADMIN));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // --- Audience grouping -----------------------------------------------

    @Test
    void alertSubscribersGroupsRidersByRole() {
        when(preferenceRepository.findByRouteIdAndScheduleAlertsEnabledTrue(4L)).thenReturn(List.of(
                preference("STUDENT", 10L, false),
                preference("TEACHER", 20L, true),
                preference("STUDENT", 11L, true)));

        Map<String, Set<Long>> subscribers = service.alertSubscribersByType(4L);

        assertEquals(Map.of("STUDENT", Set.of(10L, 11L), "TEACHER", Set.of(20L)), subscribers);
    }

    @Test
    void alertSubscribersOfFollowedRouteNobodyOptedIntoIsEmptyNotAWholeType() {
        when(preferenceRepository.findByRouteIdAndScheduleAlertsEnabledTrue(4L))
                .thenReturn(List.of());

        assertTrue(service.alertSubscribersByType(4L).isEmpty(),
                "an unfollowed route must have an empty audience, never a broadcast");
    }

    @Test
    void savedRowsReturnedForListing() {
        when(preferenceRepository.findByUserTypeAndUserId("STUDENT", 10L))
                .thenReturn(List.of(preference("STUDENT", 10L, false)));

        List<RoutePreferenceResponse> mine = service.getMyPreferences(STUDENT);

        assertEquals(1, mine.size());
        assertEquals(Long.valueOf(4L), mine.get(0).getRouteId());
        assertTrue(mine.get(0).getIsPinned());
        assertFalse(mine.get(0).getScheduleAlertsEnabled());
    }

    // --- Fixtures --------------------------------------------------------

    private static UserRoutePreference preference(String userType, Long userId, boolean alertsOn) {
        return UserRoutePreference.builder()
                .userType(userType)
                .userId(userId)
                .routeId(4L)
                .isPinned(true)
                .scheduleAlertsEnabled(alertsOn)
                .build();
    }

    private static UserRoutePreference preference(boolean pinned, boolean alerts) {
        return UserRoutePreference.builder()
                .userType("STUDENT")
                .userId(10L)
                .routeId(4L)
                .isPinned(pinned)
                .scheduleAlertsEnabled(alerts)
                .build();
    }
}
