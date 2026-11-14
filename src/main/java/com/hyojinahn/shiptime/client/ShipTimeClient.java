package com.hyojinahn.shiptime.client;

import java.util.List;
import java.util.function.Supplier;

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
import com.hyojinahn.shiptime.client.dto.ShipmentRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;
import com.hyojinahn.shiptime.config.ShipTimeProperties;

/**
 * Calls the ShipTime REST API and turns its errors into {@link ShipTimeApiException}.
 */
@Component
public class ShipTimeClient {

    static final String RATES_PATH = "/rest/rates";
    static final String SHIPMENTS_PATH = "/rest/shipments";

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
        return post(RATES_PATH, request, RateResponse.class, RateResponse::empty);
    }

    /** Creates a shipment for a quote returned by {@link #getRates}. */
    public ShipmentResponse createShipment(ShipmentRequest request) {
        return post(SHIPMENTS_PATH, request, ShipmentResponse.class, null);
    }

    /**
     * Sends {@code body} as JSON and reads the response.
     * 400 becomes REJECTED with ShipTime's messages; other failures become UNAVAILABLE.
     *
     * @param emptyResult answer for 404 or an empty body; null if those are errors
     */
    private <T> T post(String path, Object body, Class<T> type, Supplier<T> emptyResult) {
        if (!properties.configured()) {
            throw ShipTimeApiException.unavailable("ShipTime credentials are not configured", null);
        }
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((req, res) -> {
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is2xxSuccessful()) {
                            T result = res.bodyTo(type);
                            if (result != null) {
                                return result;
                            }
                            if (emptyResult != null) {
                                return emptyResult.get();
                            }
                            throw ShipTimeApiException.unavailable("ShipTime returned an empty response", null);
                        }
                        if (status.value() == HttpStatus.NOT_FOUND.value() && emptyResult != null) {
                            return emptyResult.get();
                        }
                        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
                            List<String> messages = readMessages(res);
                            log.warn("ShipTime rejected {}: {}", path, messages);
                            throw ShipTimeApiException.rejected(messages);
                        }
                        log.error("ShipTime {} failed with status {}", path, status.value());
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
