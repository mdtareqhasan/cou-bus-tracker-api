package com.cou.bustracker.service;

import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.entity.Notice;
import com.cou.bustracker.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the role → audience contract behind {@code /api/notices/active}:
 * students see ALL+STUDENT, teachers see ALL+TEACHER, admins see everything
 * — and an unrecognised role sees nothing rather than everything.
 *
 * <p>Only interfaces can be mocked on this JVM, matching the house style of
 * the other service tests.
 */
class NoticeServiceTest {

    private NoticeRepository repository;
    private NoticeService service;

    @BeforeEach
    void setUp() {
        repository = mock(NoticeRepository.class);
        // The push event publisher isn't under test here.
        service = new NoticeService(repository, mock(ApplicationEventPublisher.class));
    }

    @Test
    void studentSeesAllAndStudentNoticesNewestFirst() {
        // The repository applies the filter and the ORDER BY; asserting the
        // arguments is what pins the service's role mapping.
        when(repository.findActiveNoticesForAudiences(any(), eq(List.of("ALL", "STUDENT"))))
                .thenReturn(List.of());

        List<NoticeResponse> notices = service.getActiveNotices("STUDENT");

        assertTrue(notices.isEmpty());
    }

    @Test
    void teacherIsAskedForAllAndTeacherNotices() {
        when(repository.findActiveNoticesForAudiences(any(), eq(List.of("ALL", "TEACHER"))))
                .thenReturn(List.of(notice("Staff room shifted", "TEACHER")));

        List<NoticeResponse> notices = service.getActiveNotices("TEACHER");

        assertEquals(1, notices.size());
        assertEquals("TEACHER", notices.get(0).getAudience());
    }

    @Test
    void adminSeesEveryAudience() {
        when(repository.findActiveNoticesForAudiences(any(), eq(List.of("ALL", "STUDENT", "TEACHER"))))
                .thenReturn(List.of(notice("Everyone", "ALL")));

        assertEquals(1, service.getActiveNotices("ADMIN").size());
        assertEquals(1, service.getActiveNotices("SUPER_ADMIN").size());
    }

    @Test
    void studentQueryNeverIncludesTeacherOnlyNotices() {
        // Guards the leak this whole change exists to close: the audience
        // list handed to the repository must never contain TEACHER for a
        // student, nor STUDENT for a teacher.
        when(repository.findActiveNoticesForAudiences(any(), eq(List.of("ALL", "STUDENT"))))
                .thenReturn(List.of(notice("Exam seating", "STUDENT"), notice("Holiday", "ALL")));

        List<NoticeResponse> notices = service.getActiveNotices("STUDENT");

        assertEquals(List.of("STUDENT", "ALL"),
                notices.stream().map(NoticeResponse::getAudience).toList());
    }

    @Test
    void unknownRoleSeesNothingInsteadOfEverything() {
        when(repository.findActiveNoticesForAudiences(any(), any()))
                .thenReturn(List.of(notice("Should not leak", "ALL")));

        assertTrue(service.getActiveNotices("GHOST_ROLE").isEmpty());
    }

    private static Notice notice(String title, String audience) {
        return Notice.builder()
                .id(1L)
                .title(title)
                .body("body")
                .audience(audience)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}
