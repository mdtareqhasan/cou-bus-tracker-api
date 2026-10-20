package com.cou.bustracker.service;

import com.cou.bustracker.entity.UserFcmToken;
import com.cou.bustracker.event.NoticeCreatedEvent;
import com.cou.bustracker.event.ScheduleChangedEvent;
import com.cou.bustracker.event.ScheduleChangedEvent.Action;
import com.cou.bustracker.event.ScheduleChangedEvent.Field;
import com.cou.bustracker.event.ScheduleChangedEvent.FieldChange;
import com.cou.bustracker.repository.UserFcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies per-device-language rendering of schedule pushes against the real
 * {@code messages*.properties} bundles — the dispatcher is the only place
 * where a {@link ScheduleChangedEvent} becomes human text, so this is where
 * the en/bn contract is pinned down.
 *
 * <p>Uses a real {@link FcmTokenService} (backed by a mocked repository) and a
 * recording {@link FcmService} subclass instead of Mockito mocks: only
 * interfaces can be mocked on this JVM, matching the house style of the other
 * service tests.
 */
class NotificationDispatcherTest {

    /**
     * The {@code event()} fixture below uses busId=1 and scheduleId=1, so every
     * schedule push must deep-link to that bus page with the trip highlighted.
     */
    private static final String SCHEDULE_ROUTE = "/bus/1?scheduleId=1";

    /** One send as the dispatcher issued it: tokens + rendered text + route. */
    private record Send(List<String> tokens, String title, String body, String route) {
    }

    /** Captures {@code sendEachAsync} calls instead of talking to FCM. */
    private static class RecordingFcmService extends FcmService {
        final List<Send> sends = new ArrayList<>();

        RecordingFcmService() {
            super((FirebaseMessaging) null);
        }

        @Override
        public void sendEachAsync(List<String> tokens, String title, String body, String route,
                                  Consumer<List<String>> onUnregistered) {
            sends.add(new Send(tokens, title, body, route));
        }
    }

    private UserFcmTokenRepository repository;
    private RecordingFcmService fcmService;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = mock(UserFcmTokenRepository.class);
        fcmService = new RecordingFcmService();

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");

        dispatcher = new NotificationDispatcher(
                new FcmTokenService(repository), fcmService, messageSource);
    }

    @Test
    void rendersEachLanguageGroupInItsOwnLanguage() {
        audienceTokens(token("bn-token-1", "bn"), token("bn-token-2", "bn"), token("en-token-1", "en"));

        dispatcher.dispatch(event(Action.CREATED, List.of()));

        assertEquals(List.of(
                new Send(List.of("bn-token-1", "bn-token-2"),
                        "নতুন সময়সূচি: BUS 03",
                        "BUS 03 · সিওই ব্লু · BLUE · কান্দিরপাড় → ক্যাম্পাস · নতুন ফেরি 7:00 AM থেকে ছাড়ছে (SUN-THU)।",
                        SCHEDULE_ROUTE),
                new Send(List.of("en-token-1"),
                        "New schedule: BUS 03",
                        "BUS 03 · COU Blue · BLUE · Kandirpar → Campus · New trip departs 7:00 AM (SUN-THU).",
                        SCHEDULE_ROUTE)),
                fcmService.sends);
    }

    @Test
    void rendersUpdateAsPlainLanguageSentencesPerField() {
        audienceTokens(token("bn-token", "bn"), token("en-token", "en"));

        List<FieldChange> changes = List.of(
                new FieldChange(Field.DEPARTURE, "07:00", "07:15"),
                new FieldChange(Field.BUS_NAME_EN, null, "COU Blue"));

        dispatcher.dispatch(event(Action.UPDATED, changes));

        assertEquals(List.of(
                new Send(List.of("bn-token"),
                        "সময়সূচি হালনাগাদ: BUS 03",
                        "BUS 03 · সিওই ব্লু · BLUE · কান্দিরপাড় → ক্যাম্পাস · "
                                + "এখন 7:15 AM এ ছাড়ছে (আগে 7:00 AM)। বাসের নাম এখন “COU Blue”।",
                        SCHEDULE_ROUTE),
                new Send(List.of("en-token"),
                        "Schedule updated: BUS 03",
                        "BUS 03 · COU Blue · BLUE · Kandirpar → Campus · "
                                + "Now departs at 7:15 AM (was 7:00 AM). Bus name is now “COU Blue”.",
                        SCHEDULE_ROUTE)),
                fcmService.sends);
    }

    @Test
    void legacyTokenWithoutLanguageFallsBackToEnglishBundle() {
        // A token registered before app_language existed lands in the "en"
        // group (FcmTokenService normalizes) and must render in English.
        audienceTokens(token("legacy-token", null));

        dispatcher.dispatch(event(Action.DEACTIVATED, List.of()));

        assertEquals(List.of(new Send(
                List.of("legacy-token"),
                "Service paused: BUS 03",
                "BUS 03 · COU Blue · BLUE · Kandirpar → Campus · Paused (was departing 7:00 AM (SUN-THU)).",
                SCHEDULE_ROUTE)),
                fcmService.sends);
    }

    @Test
    void deletedBodyReadsCleanlyWithPastTenseWhen() {
        audienceTokens(token("en-token", "en"));

        dispatcher.dispatch(event(Action.DELETED, List.of()));

        // Regression: used to render "...no longer runs was departing 7:00 AM".
        assertEquals(List.of(new Send(
                List.of("en-token"),
                "Schedule removed: BUS 03",
                "BUS 03 · COU Blue · BLUE · Kandirpar → Campus · No longer runs (was departing 7:00 AM (SUN-THU)).",
                SCHEDULE_ROUTE)),
                fcmService.sends);
    }

    @Test
    void skipsInactiveSchedule() {
        audienceTokens(token("en-token", "en"));

        // Inactive CREATED: admin bookkeeping, not news.
        dispatcher.dispatch(new ScheduleChangedEvent(
                Action.CREATED, 1L, 1L, "BUS 03", "সিওই ব্লু", "COU Blue", "BLUE", "UP",
                4L, "কান্দিরপাড় → ক্যাম্পাস", "Kandirpar → Campus",
                LocalTime.of(7, 0), null, "SUN-THU",
                false, List.of()));

        assertTrue(fcmService.sends.isEmpty());
    }

    @Test
    void skipsUnmappedCategoryInsteadOfBroadcasting() {
        audienceTokens(token("en-token", "en"));

        dispatcher.dispatch(new ScheduleChangedEvent(
                Action.CREATED, 2L, 1L, "BUS 03", null, null, "VIP", "UP",
                null, null, null, LocalTime.of(7, 0), null, null,
                true, List.of()));

        assertTrue(fcmService.sends.isEmpty());
    }

    // --- Notice pushes -------------------------------------------------

    @Test
    void noticeForAllFansOutToStudentsAndTeachers() {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT"))
                .thenReturn(List.of(token("student-token", "bn")));
        when(repository.findByUserTypeAndIsActiveTrue("TEACHER"))
                .thenReturn(List.of(token("teacher-token", "en")));

        dispatcher.dispatch(noticeEvent("ALL", "Semester break notice"));

        assertEquals(List.of(
                new Send(List.of("student-token"),
                        "Semester break notice", "Campus closed Friday.", "/notices"),
                new Send(List.of("teacher-token"),
                        "Semester break notice", "Campus closed Friday.", "/notices")),
                fcmService.sends);
    }

    @Test
    void noticeForSingleAudienceSkipsTheOtherRole() {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT"))
                .thenReturn(List.of(token("student-token", "en")));

        dispatcher.dispatch(noticeEvent("STUDENT", "Exam seating", "Room 204 tomorrow."));

        assertEquals(List.of(new Send(
                List.of("student-token"), "Exam seating", "Room 204 tomorrow.", "/notices")),
                fcmService.sends);
    }

    @Test
    void noticeTextIsPushedVerbatimInEveryLanguageGroup() {
        // Notice prose is admin-authored (often Bengali) — the dispatcher
        // must not translate or wrap it, only route it to the right tokens.
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT"))
                .thenReturn(List.of(token("bn-token", "bn"), token("en-token", "en")));

        dispatcher.dispatch(noticeEvent("STUDENT", "রুট পরিবর্তন", "শুক্রবার বাস চলবে না।"));

        assertEquals(List.of(
                new Send(List.of("bn-token", "en-token"),
                        "রুট পরিবর্তন", "শুক্রবার বাস চলবে না।", "/notices")),
                fcmService.sends);
    }

    @Test
    void unknownNoticeAudienceIsSkippedInsteadOfBroadcasting() {
        dispatcher.dispatch(noticeEvent("VIP", "Classified"));

        assertTrue(fcmService.sends.isEmpty());
    }

    @Test
    void nullNoticeAudienceDefaultsToAllRiders() {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT"))
                .thenReturn(List.of(token("student-token", "en")));
        when(repository.findByUserTypeAndIsActiveTrue("TEACHER"))
                .thenReturn(List.of(token("teacher-token", "en")));

        dispatcher.dispatch(noticeEvent(null, "General holiday"));

        assertEquals(2, fcmService.sends.size());
    }

    @Test
    void longNoticeBodyIsTruncatedForTheTray() {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT"))
                .thenReturn(List.of(token("student-token", "en")));

        String longBody = "x".repeat(600);
        dispatcher.dispatch(noticeEvent("STUDENT", "Long notice", longBody));

        String sent = fcmService.sends.get(0).body();
        assertEquals(500, sent.length());
        assertTrue(sent.endsWith("…"));
    }

    private void audienceTokens(UserFcmToken... tokens) {
        when(repository.findByUserTypeAndIsActiveTrue("STUDENT")).thenReturn(List.of(tokens));
    }

    private static NoticeCreatedEvent noticeEvent(String audience, String title) {
        return new NoticeCreatedEvent(7L, title, "Campus closed Friday.", audience,
                LocalDateTime.of(2026, 10, 20, 12, 0));
    }

    private static NoticeCreatedEvent noticeEvent(String audience, String title, String body) {
        return new NoticeCreatedEvent(7L, title, body, audience,
                LocalDateTime.of(2026, 10, 20, 12, 0));
    }

    private static UserFcmToken token(String token, String appLanguage) {
        return UserFcmToken.builder()
                .userType("STUDENT")
                .userId(10L)
                .token(token)
                .appLanguage(appLanguage)
                .build();
    }

    /** An active BLUE-bus schedule — the audience resolves to STUDENT. */
    private static ScheduleChangedEvent event(Action action, List<FieldChange> changes) {
        return new ScheduleChangedEvent(
                action, 1L, 1L, "BUS 03", "সিওই ব্লু", "COU Blue", "BLUE", "UP",
                4L, "কান্দিরপাড় → ক্যাম্পাস", "Kandirpar → Campus",
                LocalTime.of(7, 0), null, "SUN-THU",
                true, changes);
    }
}
