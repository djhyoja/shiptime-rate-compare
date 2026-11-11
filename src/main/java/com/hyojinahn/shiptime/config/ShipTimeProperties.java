package com.hyojinahn.shiptime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection settings for the ShipTime REST API.
 * Credentials are supplied through environment variables and are never committed.
 */
@ConfigurationProperties(prefix = "shiptime")
public record ShipTimeProperties(
        String baseUrl,
        String username,
        String password,
        @DefaultValue("5000") int connectTimeoutMs,
        @DefaultValue("15000") int readTimeoutMs
) {

    public boolean configured() {
        return notBlank(baseUrl) && notBlank(username) && notBlank(password);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
