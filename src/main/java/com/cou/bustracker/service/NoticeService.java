package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateNoticeRequest;
import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.entity.Notice;
import com.cou.bustracker.event.NoticeCreatedEvent;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    /**
     * Audiences a notice can target. Deliberately the same vocabulary as
     * {@code user_fcm_tokens.user_type} (STUDENT / TEACHER) plus ALL, so a
     * push fan-out can map an audience straight onto FCM token groups.
     */
    private static final Set<String> SUPPORTED_AUDIENCES = Set.of("STUDENT", "TEACHER", "ALL");

    /** Used when a request doesn't name an audience — everyone. */
    public static final String DEFAULT_AUDIENCE = "ALL";

    /**
     * Which stored audiences a role is allowed to see.
     *
     * <p>Riders only ever see {@code ALL} plus their own role's notices;
     * admins and super-admins manage every notice, so they see all three.
     * The role key is the JWT claim ({@code CurrentUser#role}), the value is
     * what goes into {@code notices.audience}.
     */
    private static final Map<String, List<String>> AUDIENCES_BY_ROLE = Map.of(
            "STUDENT", List.of(DEFAULT_AUDIENCE, "STUDENT"),
            "TEACHER", List.of(DEFAULT_AUDIENCE, "TEACHER"),
            "ADMIN", List.of(DEFAULT_AUDIENCE, "STUDENT", "TEACHER"),
            "SUPER_ADMIN", List.of(DEFAULT_AUDIENCE, "STUDENT", "TEACHER"));

    private final NoticeRepository noticeRepository;

    /**
     * Publishes {@link NoticeCreatedEvent} from inside the writing
     * transaction; {@code NoticeCreatedListener} forwards it to the async
     * push dispatcher only after commit.
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Active, non-expired notices the given role is allowed to see, newest
     * first.
     *
     * <p>The audience filter keeps a student from reading teacher-only
     * notices (and vice versa) — the same split the push fan-out applies.
     * An unrecognised role sees nothing rather than everything: the security
     * layer rejects it anyway, and failing open here would turn any future
     * mapping mistake into an information leak.
     *
     * @param role the JWT role claim ({@code CurrentUser#role})
     */
    public List<NoticeResponse> getActiveNotices(String role) {
        List<String> audiences = AUDIENCES_BY_ROLE.get(role);
        if (audiences == null) {
            return List.of();
        }
        List<Notice> notices = noticeRepository.findActiveNoticesForAudiences(LocalDateTime.now(), audiences);
        return notices.stream()
                .map(this::mapToNoticeResponse)
                .collect(Collectors.toList());
    }

    public List<NoticeResponse> getAllNoticesForAdmin() {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();
        return notices.stream()
                .map(this::mapToNoticeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeResponse createNotice(CreateNoticeRequest request) {
        Integer expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : 24;

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .expiryHours(expiryHours)
                .audience(resolveAudience(request.getAudience()))
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        // Published inside the transaction; the listener fires only AFTER_COMMIT,
        // so riders are never told about a notice that rolled back.
        eventPublisher.publishEvent(NoticeCreatedEvent.from(savedNotice));
        return mapToNoticeResponse(savedNotice);
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found with id: " + id));
        noticeRepository.delete(notice);
    }

    @Transactional
    public void deactivateExpiredNotices() {
        List<Notice> expiredNotices = noticeRepository.findActiveNotices(LocalDateTime.now());
        // Notices not returned by findActiveNotices are already expired
    }

    /**
     * Normalizes the requested audience to the stored uppercase form.
     * Blank/null becomes {@link #DEFAULT_AUDIENCE} so older clients that
     * don't send the field keep their broadcast behavior; anything outside
     * the supported set is rejected (the request-level {@code @Pattern}
     * catches this first, but internal callers skip bean validation).
     */
    private static String resolveAudience(String requested) {
        if (requested == null || requested.isBlank()) {
            return DEFAULT_AUDIENCE;
        }
        String audience = requested.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_AUDIENCES.contains(audience)) {
            throw new IllegalArgumentException(
                    "Unsupported notice audience: " + requested + " (expected STUDENT, TEACHER or ALL)");
        }
        return audience;
    }

    private NoticeResponse mapToNoticeResponse(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .audience(notice.getAudience())
                .isActive(notice.getIsActive())
                .createdAt(notice.getCreatedAt())
                .expiresAt(notice.getExpiresAt())
                .build();
    }
}
