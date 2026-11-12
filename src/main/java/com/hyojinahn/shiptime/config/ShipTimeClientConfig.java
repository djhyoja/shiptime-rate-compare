package com.hyojinahn.shiptime.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * How to connect to ShipTime: base URL, Basic Auth and timeouts.
 * {@link com.hyojinahn.shiptime.client.ShipTimeClient} only decides what to call.
 */
@Configuration
public class ShipTimeClientConfig {

    @Bean
    public RestClient shipTimeRestClient(RestClient.Builder builder, ShipTimeProperties properties) {
        ClientHttpRequestFactorySettings timeouts = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        return configure(builder, properties)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts))
                .build();
    }

    /**
     * Applies base URL, JSON accept header and Basic Auth.
     * Tests reuse this so they check the same headers the application sends.
     */
    public static RestClient.Builder configure(RestClient.Builder builder, ShipTimeProperties properties) {
        builder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (hasText(properties.baseUrl())) {
            builder.baseUrl(properties.baseUrl());
        }
        if (properties.configured()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION,
                    basicAuth(properties.username(), properties.password()));
        }
        return builder;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    private static String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
