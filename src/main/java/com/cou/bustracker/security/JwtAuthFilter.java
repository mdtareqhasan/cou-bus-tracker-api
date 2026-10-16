package com.cou.bustracker.security;

import com.cou.bustracker.repository.AdminRepository;
import com.cou.bustracker.repository.StudentRepository;
import com.cou.bustracker.repository.SuperAdminRepository;
import com.cou.bustracker.repository.TeacherRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SuperAdminRepository superAdminRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractUsername(jwt);
            final String role = jwtService.extractRole(jwt);
            if (userEmail != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (isUserValid(userEmail, role) && jwtService.validateToken(jwt, new User(userEmail, "", List.of(new SimpleGrantedAuthority("ROLE_" + role))))) {
                    User userDetails = new User(userEmail, "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Invalid/expired tokens simply remain unauthenticated.
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUserValid(String principal, String role) {
        return switch (role) {
            case "ADMIN" -> adminRepository.findByEmail(principal).isPresent();
            case "STUDENT" -> {
                var student = principal.contains("@")
                        ? studentRepository.findByEmail(principal)
                        : studentRepository.findByPhone(principal);
                yield student.map(s -> Boolean.TRUE.equals(s.getIsActive())).orElse(false);
            }
            case "TEACHER" -> {
                var teacher = principal.contains("@")
                        ? teacherRepository.findByEmail(principal)
                        : teacherRepository.findByPhone(principal);
                yield teacher.map(t -> Boolean.TRUE.equals(t.getIsActive())).orElse(false);
            }
            case "SUPER_ADMIN" -> superAdminRepository.findByEmail(principal)
                    .map(superAdmin -> Boolean.TRUE.equals(superAdmin.getIsActive()))
                    .orElse(false);
            default -> false;
        };
    }
}
