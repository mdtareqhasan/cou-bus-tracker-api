package com.cou.bustracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.TeacherRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side half of the "no department/roll/session (students) or employee
 * ID/department/designation (teachers) — no app" policy.
 *
 * <p>A student or teacher whose profile is still missing those values gets
 * {@code 403} with {@code code = PROFILE_INCOMPLETE} instead of the response
 * body, so the Flutter app can route them to the completion screen rather than
 * treat it as a dead session.
 *
 * <p>The whole {@code /api/auth/**} subtree is exempt: it holds the login, OTP
 * and password-reset flows (which must work for an incomplete profile — they
 * are how such a profile gets completed) as well as the profile read/update
 * endpoints the completion flow itself depends on. {@code /api/academic/**}
 * is exempt for the same reason: the department/office/session pickers are
 * read on the completion screen itself. Guest-facing endpoints
 * ({@code /buses}, {@code /schedules}) carry no token and are unaffected;
 * {@code /notices/active} <em>is</em> authenticated (its audience filter
 * needs the caller's role), so an incomplete profile is held back there too.
 *
 * <p>Runs as an MVC interceptor so it is guaranteed to execute after
 * {@link JwtAuthFilter} has populated the {@link SecurityContextHolder}.
 */
@Component
@RequiredArgsConstructor
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    /** Machine-readable marker the Flutter app switches on. */
    public static final String ERROR_CODE = "PROFILE_INCOMPLETE";

    /** Account-lifecycle endpoints an incomplete profile may still use. */
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    /**
     * Public reference data an incomplete profile needs in order to complete
     * itself: the department/office/session pickers are read on the very
     * screen that fills in the missing academic details, so blocking them
     * would deadlock the completion flow.
     */
    private static final String ACADEMIC_PATH_PREFIX = "/api/academic/";

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = roleOf(authentication);
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith(AUTH_PATH_PREFIX) || path.startsWith(ACADEMIC_PATH_PREFIX)) {
            return true;
        }

        String principal = authentication.getName();
        boolean incomplete = "STUDENT".equals(role)
                ? isIncompleteStudent(principal)
                : isIncompleteTeacher(principal);
        if (!incomplete) {
            return true;
        }

        writeForbidden(response, role);
        return false;
    }

    /** @return {@code true} when department, roll number or session is missing */
    private boolean isIncompleteStudent(String principal) {
        return studentRepository.findByEmail(principal)
                .map(student -> student.getDepartment() == null
                        || isBlank(student.getRollNumber())
                        || student.getSession() == null)
                // Row resolved by JwtAuthFilter; if it vanished, let the request
                // through and let the endpoint report the real problem.
                .orElse(false);
    }

    /** @return {@code true} when department, employee ID or designation is missing */
    private boolean isIncompleteTeacher(String principal) {
        return teacherRepository.findByEmail(principal)
                .map(teacher -> teacher.getDepartment() == null
                        || isBlank(teacher.getTeacherId())
                        || isBlank(teacher.getDesignation()))
                .orElse(false);
    }

    /** Reads {@code ROLE_<name>} — the authority {@link JwtAuthFilter} sets. */
    private String roleOf(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    private void writeForbidden(HttpServletResponse response, String role) throws IOException {
        String message = "STUDENT".equals(role)
                ? "Complete your department, roll number and session to use your account"
                : "Complete your department, employee ID and designation to use your account";

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", message);
        body.put("code", ERROR_CODE);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
