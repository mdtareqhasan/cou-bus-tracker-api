package com.cou.bustracker.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class FcmService {

    /** FCM accepts at most 500 messages per sendEach() call. */
    public static final int MAX_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;

    /**
     * {@code @Autowired} is required now that a package-private test
     * constructor exists: with two constructors and no marker, Spring can't
     * pick one and startup fails.
     */
    @Autowired
    public FcmService(FirebaseApp firebaseApp) {
        this(FirebaseMessaging.getInstance(firebaseApp));
    }

    /** Package-private seam so tests can substitute the messaging client. */
    FcmService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Enqueues a multicast push and returns immediately — this method does
     * <em>not</em> wait for FCM, so the caller's thread is released as soon as
     * the batch has been handed to the SDK's own transport layer.
     *
     * <p>Results are reported asynchronously on FCM's worker thread (or inline
     * if the call already finished), never on the calling thread. Failures are
     * summarised at INFO; tokens FCM reports as dead are collected and passed
     * to {@code onUnregistered} so the caller can prune them.
     *
     * @param tokens         1..{@link #MAX_BATCH_SIZE} registration tokens
     * @param onUnregistered receives tokens that no longer exist on FCM's side
     *                       (may be called with an empty list — check first)
     */
    public void sendEachAsync(List<String> tokens, String title, String body,
                              Consumer<List<String>> onUnregistered) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        if (tokens.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "sendEachAsync takes at most " + MAX_BATCH_SIZE + " tokens, got " + tokens.size());
        }

        List<Message> messages = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            messages.add(Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build());
        }

        ApiFuture<BatchResponse> future = firebaseMessaging.sendEachAsync(messages);
        future.addListener(() -> reportBatch(tokens, future, onUnregistered), Runnable::run);
    }

    private void reportBatch(List<String> tokens, ApiFuture<BatchResponse> future,
                             Consumer<List<String>> onUnregistered) {
        try {
            BatchResponse response = future.get();

            List<String> deadTokens = new ArrayList<>();
            List<String> transientErrors = new ArrayList<>();

            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse result = responses.get(i);
                if (result.isSuccessful()) {
                    continue;
                }
                FirebaseMessagingException error = result.getException();
                String reason = describe(error);

                if (error != null && error.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deadTokens.add(tokens.get(i));
                } else {
                    transientErrors.add(reason);
                }
                log.warn("FCM delivery failed for token {}…: {}", abbreviate(tokens.get(i)), reason);
            }

            log.info("FCM batch done: {} ok, {} failed ({} dead token(s))",
                    response.getSuccessCount(), response.getFailureCount(), deadTokens.size());

            if (!deadTokens.isEmpty() && onUnregistered != null) {
                try {
                    onUnregistered.accept(deadTokens);
                } catch (Exception pruneFailure) {
                    log.error("Failed to prune unregistered FCM tokens: {}",
                            pruneFailure.getMessage(), pruneFailure);
                }
            }
            if (!transientErrors.isEmpty()) {
                log.warn("FCM batch had {} non-fatal failure(s); tokens left in place for retry",
                        transientErrors.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for FCM batch result");
        } catch (Exception e) {
            log.error("FCM batch send failed: {}", e.getMessage(), e);
        }
    }

    private static String describe(FirebaseMessagingException error) {
        if (error == null) {
            return "unknown error";
        }
        MessagingErrorCode code = error.getMessagingErrorCode();
        return (code != null ? code.name() : String.valueOf(error.getErrorCode()))
                + " (" + error.getMessage() + ")";
    }

    private static String abbreviate(String token) {
        return token.length() <= 12 ? token : token.substring(0, 12);
    }
}
