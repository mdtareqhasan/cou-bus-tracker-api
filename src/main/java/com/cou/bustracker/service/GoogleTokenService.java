package com.cou.bustracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/** Validates a Google ID token server-side; the Flutter client is never trusted by itself. */
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
            throw new BadCredentialsException(
                    "গুগল সাইন-ইন টোকেন যাচাই করা যায়নি। আবার চেষ্টা করুন।", exception);
        }

        if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
            throw new BadCredentialsException(
                    "গুগল সাইন-ইন টোকেন সঠিক নয় অথবা ইমেইল যাচাইকৃত নয়।");
        }

        String email = token.getPayload().getEmail();
        if (email == null || !isAllowedInstitutionalEmail(email)) {
            throw new BadCredentialsException(
                    "শুধুমাত্র কুমিল্লা বিশ্ববিদ্যালয়ের প্রাতিষ্ঠানিক ইমেইল "
                            + "(name@cou.ac.bd অথবা name@stud.cou.ac.bd) দিয়ে প্রবেশ করা যাবে।");
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
