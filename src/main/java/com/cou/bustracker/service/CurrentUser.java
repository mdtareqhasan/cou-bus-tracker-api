package com.cou.bustracker.service;

/**
 * Lightweight holder for the authenticated caller, resolved from the JWT.
 *
 * @param userId the database id of the resolved student/teacher/admin
 * @param role   the JWT role claim, e.g. {@code STUDENT}, {@code TEACHER},
 *               {@code ADMIN}, {@code SUPER_ADMIN}
 * @param email  the authenticated principal (email)
 */
public record CurrentUser(Long userId, String role, String email) {
}
