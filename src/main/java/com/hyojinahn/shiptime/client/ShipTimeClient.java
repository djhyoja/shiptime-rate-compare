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
    static final String LABEL_PATH = "/rest/shipments/{shipId}/label";

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
     * Downloads the label PDF of a shipment. The URL is built from the configured
     * base URL and the shipment ID; the {@code labelUrl} in the shipment response is not followed,
     * so the credentials only go to the configured host.
     */
    public byte[] getLabel(long shipId) {
        checkConfigured();
        try {
            return restClient.get()
                    .uri(LABEL_PATH, shipId)
                    .accept(MediaType.APPLICATION_PDF, MediaType.ALL)
                    .exchange((req, res) -> {
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is2xxSuccessful()) {
                            byte[] pdf = res.bodyTo(byte[].class);
                            if (pdf != null && pdf.length > 0) {
                                return pdf;
                            }
                            throw ShipTimeApiException.unavailable("ShipTime returned an empty label", null);
                        }
                        log.error("ShipTime label for shipment {} failed with status {}", shipId, status.value());
                        throw ShipTimeApiException.unavailable("ShipTime returned " + status.value(), null);
                    });
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    /**
     * Sends {@code body} as JSON and reads the response.
     * 400 becomes REJECTED with ShipTime's messages; other failures become UNAVAILABLE.
     *
     * @param emptyResult answer for 404 or an empty body; null if those are errors
     */
    private <T> T post(String path, Object body, Class<T> type, Supplier<T> emptyResult) {
        checkConfigured();
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
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    private void checkConfigured() {
        if (!properties.configured()) {
            throw ShipTimeApiException.unavailable("ShipTime credentials are not configured", null);
        }
    }

    private static ShipTimeApiException translate(RestClientException e) {
        if (e instanceof ResourceAccessException) {
            log.error("Cannot reach ShipTime: {}", e.getMessage());
            return ShipTimeApiException.unavailable("Cannot reach ShipTime", e);
        }
        log.error("Unexpected ShipTime response: {}", e.getMessage());
        return ShipTimeApiException.unavailable("Unexpected ShipTime response", e);
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
