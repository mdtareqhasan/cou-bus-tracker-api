package com.cou.bustracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/** Validates a Google ID token server-side; the Flutter client is never trusted by itself. */
@Service
public class GoogleTokenService {
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(@Value("${google.oauth2.client-id}") String clientId) {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdentity verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
                throw new BadCredentialsException("Google sign-in token is invalid or email is not verified");
            }
            return new GoogleIdentity(token.getPayload().getSubject(), token.getPayload().getEmail());
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadCredentialsException("Unable to verify Google sign-in token", exception);
        }
    }

    public record GoogleIdentity(String subject, String email) { }
}
