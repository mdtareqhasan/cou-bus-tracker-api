package com.cou.bustracker.service;

import com.cou.bustracker.exception.UnauthorizedException;
import com.cou.bustracker.repository.AdminRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.SuperAdminRepository;
import com.cou.bustracker.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Resolves the caller's database identity from the authenticated
 * {@link Authentication} set by {@code JwtAuthFilter}.
 *
 * <p>The JWT carries the user's email (or phone) as the subject and the role
 * as a {@code role} claim, which the filter turns into a
 * {@code ROLE_<role>} authority. This service maps that back to the concrete
 * student/teacher/admin row so controllers can act on the real user id.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;

    /**
     * @return the resolved {@link CurrentUser}
     * @throws UnauthorizedException if there is no authenticated principal, an
     *         unrecognised role, or the user no longer exists / is inactive
     */
    public CurrentUser resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        String principal = authentication.getName();
        String role = extractRole(authentication);

        Long userId = switch (role) {
            case "STUDENT" -> studentRepository
                    .findByPhoneOrEmail(principal)
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .map(s -> s.getId())
                    .orElseThrow(() -> new UnauthorizedException("Student not found or inactive"));
            case "TEACHER" -> teacherRepository
                    .findByPhoneOrEmail(principal)
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .map(t -> t.getId())
                    .orElseThrow(() -> new UnauthorizedException("Teacher not found or inactive"));
            case "ADMIN" -> adminRepository
                    .findByEmail(principal)
                    .map(a -> a.getId())
                    .orElseThrow(() -> new UnauthorizedException("Admin not found"));
            case "SUPER_ADMIN" -> superAdminRepository
                    .findByEmail(principal)
                    .filter(sa -> Boolean.TRUE.equals(sa.getIsActive()))
                    .map(sa -> sa.getId())
                    .orElseThrow(() -> new UnauthorizedException("Super admin not found or inactive"));
            default -> throw new UnauthorizedException("Unsupported role: " + role);
        };

        return new CurrentUser(userId, role, principal);
    }

    /** Reads the role from the {@code ROLE_<role>} authority set by the JWT filter. */
    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && auth.startsWith("ROLE_"))
                .map(auth -> auth.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("No role found in token"));
    }
}
