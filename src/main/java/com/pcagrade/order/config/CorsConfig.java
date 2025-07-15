package com.pcagrade.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // URLs autorisées pour votre frontend
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",     // Tous les ports localhost
                "http://127.0.0.1:*",     // Tous les ports 127.0.0.1
                "https://localhost:*"     // HTTPS localhost si besoin
        ));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers autorisés
        config.setAllowedHeaders(Arrays.asList("*"));

        // Pas de credentials pour simplifier
        config.setAllowCredentials(false);

        // Cache preflight requests
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
