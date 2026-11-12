package com.hyojinahn.shiptime.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hyojinahn.shiptime.client.dto.ErrorBody;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.RateResponse;
import com.hyojinahn.shiptime.config.ShipTimeProperties;

/**
 * Calls the ShipTime REST API and turns its errors into {@link ShipTimeApiException}.
 */
@Component
public class ShipTimeClient {

    static final String RATES_PATH = "/rest/rates";

    private static final Logger log = LoggerFactory.getLogger(ShipTimeClient.class);

    private final RestClient restClient;
    private final ShipTimeProperties properties;

    public ShipTimeClient(RestClient shipTimeRestClient, ShipTimeProperties properties) {
        this.restClient = shipTimeRestClient;
        this.properties = properties;
    }

    /**
     * Returns the available rates. "No rates found" (404) is a normal answer
     * and comes back as an empty response, not as an error.
     */
    public RateResponse getRates(RateRequest request) {
        if (!properties.configured()) {
            throw ShipTimeApiException.unavailable("ShipTime credentials are not configured", null);
        }
        try {
            return restClient.post()
                    .uri(RATES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> {
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is2xxSuccessful()) {
                            RateResponse body = res.bodyTo(RateResponse.class);
                            return body == null ? RateResponse.empty() : body;
                        }
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            return RateResponse.empty();
                        }
                        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
                            List<String> messages = readMessages(res);
                            log.warn("ShipTime rejected rate request: {}", messages);
                            throw ShipTimeApiException.rejected(messages);
                        }
                        log.error("ShipTime rate request failed with status {}", status.value());
                        throw ShipTimeApiException.unavailable("ShipTime returned " + status.value(), null);
                    });
        } catch (ResourceAccessException e) {
            log.error("Cannot reach ShipTime: {}", e.getMessage());
            throw ShipTimeApiException.unavailable("Cannot reach ShipTime", e);
        } catch (RestClientException e) {
            log.error("Unexpected ShipTime response: {}", e.getMessage());
            throw ShipTimeApiException.unavailable("Unexpected ShipTime response", e);
        }
    }

    private static List<String> readMessages(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) {
        try {
            ErrorBody body = res.bodyTo(ErrorBody.class);
            return body == null || body.messages() == null ? List.of() : body.messages();
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
