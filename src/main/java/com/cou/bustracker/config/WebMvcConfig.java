package com.cou.bustracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:56545",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5174",
                        "http://127.0.0.1:56545",
                        "https://cou-bus-tracker-backend-admin-frontend-1.onrender.com",
                        "https://cou-bus-tracker-backend-admin-frontend.onrender.com",
                        "https://co-u-bus-tracker-flutter-chi.vercel.app",
                        "https://cou-bus-tracker-super-admin.vercel.app",
                        "https://cou-super-admin.vercel.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
