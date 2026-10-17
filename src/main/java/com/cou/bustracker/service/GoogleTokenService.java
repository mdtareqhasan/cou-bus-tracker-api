package com.cou.bustracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.cou.bustracker.exception.ApiException;
import com.cou.bustracker.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validates a Google ID token server-side; the Flutter client is never trusted by itself. */
@Slf4j
@Service
public class GoogleTokenService {

    /** Teachers/staff sign in with {@code name@cou.ac.bd}. */
    public static final String FACULTY_EMAIL_DOMAIN = "cou.ac.bd";

    /** Students sign in with {@code name@stud.cou.ac.bd}. */
    public static final String STUDENT_EMAIL_DOMAIN = "stud.cou.ac.bd";

    /**
     * Only institutional Comilla University addresses are accepted:
     * students use {@code name@stud.cou.ac.bd} and teachers/staff use
     * {@code name@cou.ac.bd}.
     */
    public static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(FACULTY_EMAIL_DOMAIN, STUDENT_EMAIL_DOMAIN);

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(@Value("${google.oauth2.client-id}") String clientId) {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifies the Google ID token's signature, audience and issuer, checks the
     * email is verified, and enforces the institutional email domain.
     *
     * @throws BadCredentialsException when the token is invalid, the email is
     *         unverified, or the domain is not an allowed university domain
     */
    public GoogleIdentity verify(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (Exception exception) {
            log.warn("Google ID token verification failed: {}", exception.getMessage());
            throw new ApiException(ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED,
                    ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED.getDefaultMessage(), exception);
        }

        // verify() returns null — without throwing — for an expired token, a
        // wrong audience, a bad signature or a malformed token. Decode the
        // UNVERIFIED payload purely for diagnostics so the log says which one
        // it was; the claims are never trusted, and the token is never logged.
        if (token == null) {
            log.warn("Google verifier returned null — token expired, wrong audience, bad signature "
                    + "or malformed. Unverified claims for diagnosis: {}", describeUntrustedClaims(idToken));
            throw new ApiException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }

        if (!Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
            log.warn("Google token valid but email_verified is not true for domain '@{}' "
                    + "(aud={})", domainOf(token.getPayload().getEmail()), token.getPayload().getAudience());
            throw new ApiException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }

        String email = token.getPayload().getEmail();
        if (email == null || !isAllowedInstitutionalEmail(email)) {
            throw new ApiException(ErrorCode.GOOGLE_EMAIL_NOT_ALLOWED);
        }

        // GoogleIdToken.Payload exposes typed getters only for standard claims
        // (sub, email, ...); the OIDC "name" claim lives in the underlying map.
        Object name = token.getPayload().get("name");
        return new GoogleIdentity(
                token.getPayload().getSubject(),
                email.toLowerCase(Locale.ROOT),
                name instanceof String displayName ? displayName : null);
    }

    /** @return true when {@code email} ends with one of the allowed university domains. */
    public static boolean isAllowedInstitutionalEmail(String email) {
        String domain = domainOf(email);
        return domain != null && ALLOWED_EMAIL_DOMAINS.contains(domain);
    }

    /**
     * @return true when {@code email} belongs to the faculty/staff domain
     *         ({@code name@cou.ac.bd}); student addresses
     *         ({@code name@stud.cou.ac.bd}) return false.
     */
    public static boolean isFacultyEmail(String email) {
        return FACULTY_EMAIL_DOMAIN.equals(domainOf(email));
    }

    /** @return the lower-cased domain after the last {@code @}, or null when malformed. */
    private static String domainOf(String email) {
        if (email == null) {
            return null;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Best-effort decode of a JWT's payload segment for log output ONLY. The
     * data is unverified — never use it for any decision — and the token
     * itself is never logged, only a few non-secret claims. This is what turns
     * "token invalid" into "expired at 10:03, server clock skew?" or "audience
     * is the iOS client, not the web client" in the logs.
     */
    private static String describeUntrustedClaims(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return "payload unreadable (not a 3-segment JWT)";
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = new ObjectMapper().readValue(payload, Map.class);

            return "aud=" + claims.get("aud")
                    + ", iss=" + claims.get("iss")
                    + ", exp=" + epochSecondsToInstant(claims.get("exp"))
                    + ", iat=" + epochSecondsToInstant(claims.get("iat"))
                    + ", email_verified=" + claims.get("email_verified")
                    + ", email_domain=@" + domainOf(claims.get("email") instanceof String e ? e : null);
        } catch (Exception e) {
            return "payload unreadable (" + e.getMessage() + ")";
        }
    }

    private static String epochSecondsToInstant(Object epochSeconds) {
        if (!(epochSeconds instanceof Number number)) {
            return "unknown";
        }
        try {
            return Instant.ofEpochSecond(number.longValue()) + " (" + number.longValue() + ")";
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }

    /** Verified Google identity extracted from an ID token. */
    public record GoogleIdentity(String subject, String email, String name) {

        /** Google display name, falling back to the email's local part when absent. */
        public String displayName() {
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
    }
}
