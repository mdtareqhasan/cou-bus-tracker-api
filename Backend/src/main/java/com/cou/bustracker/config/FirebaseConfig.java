
package com.cou.bustracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");

        if (serviceAccountPath == null ||
                serviceAccountPath.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_SERVICE_ACCOUNT_PATH is not configured");
        }

        try (InputStream serviceAccount = new FileInputStream(serviceAccountPath)) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }
}
