package com.hyojinahn.shiptime.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.RateResponse;
import com.hyojinahn.shiptime.config.ShipTimeClientConfig;
import com.hyojinahn.shiptime.config.ShipTimeProperties;

class ShipTimeClientTest {

    private static final String BASE_URL = "https://shiptime.test";
    // Base64 of "user@example.com:secret"
    private static final String EXPECTED_AUTH = "Basic dXNlckBleGFtcGxlLmNvbTpzZWNyZXQ=";

    private final ShipTimeProperties properties =
            new ShipTimeProperties(BASE_URL, "user@example.com", "secret", 1000, 1000);

    private MockRestServiceServer server;
    private ShipTimeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = ShipTimeClientConfig.configure(RestClient.builder(), properties);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ShipTimeClient(builder.build(), properties);
    }

    @Test
    void sendsRequiredFieldsWithBasicAuthAndReadsRates() throws IOException {
        server.expect(requestTo(BASE_URL + "/rest/rates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, EXPECTED_AUTH))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from.postalCode").value("L6J1H6"))
                .andExpect(jsonPath("$.to.city").value("Toronto"))
                .andExpect(jsonPath("$.lineItems[0].weight").value(2.5))
                .andExpect(jsonPath("$.packageType").value("PACKAGE"))
                .andExpect(jsonPath("$.unitOfMeasurement").value("METRIC"))
                .andExpect(jsonPath("$.shipDate").value("2026-11-12"))
                .andRespond(withSuccess(sampleResponse(), MediaType.APPLICATION_JSON));

        RateResponse response = client.getRates(sampleRequest());

        assertThat(response.rates()).hasSize(3);
        assertThat(response.rates().get(0).carrierName()).isEqualTo("Precision");
        assertThat(response.rates().get(0).totalCharge().amount()).isEqualTo(1881);
        assertThat(response.rates().get(0).quoteId()).isEqualTo("72db06b5271648737882e6829dcddbce");
        server.verify();
    }

    @Test
    void noRatesFoundIsAnEmptyResult() {
        server.expect(requestTo(BASE_URL + "/rest/rates")).andRespond(withResourceNotFound());

        assertThat(client.getRates(sampleRequest()).rates()).isEmpty();
    }

    @Test
    void badRequestIsRejectedWithShipTimeMessages() {
        server.expect(requestTo(BASE_URL + "/rest/rates"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messages\":[\"Invalid postal code\"]}"));

        assertThatThrownBy(() -> client.getRates(sampleRequest()))
                .isInstanceOfSatisfying(ShipTimeApiException.class, e -> {
                    assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.REJECTED);
                    assertThat(e.details()).containsExactly("Invalid postal code");
                });
    }

    @Test
    void badRequestWithoutJsonBodyIsStillRejected() {
        server.expect(requestTo(BASE_URL + "/rest/rates"))
                .andRespond(withBadRequest().contentType(MediaType.TEXT_PLAIN).body("bad"));

        assertThatThrownBy(() -> client.getRates(sampleRequest()))
                .isInstanceOfSatisfying(ShipTimeApiException.class, e -> {
                    assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.REJECTED);
                    assertThat(e.details()).isEmpty();
                });
    }

    @Test
    void unauthorizedIsUnavailable() {
        server.expect(requestTo(BASE_URL + "/rest/rates")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getRates(sampleRequest()))
                .isInstanceOfSatisfying(ShipTimeApiException.class,
                        e -> assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.UNAVAILABLE));
    }

    @Test
    void serverErrorIsUnavailable() {
        server.expect(requestTo(BASE_URL + "/rest/rates")).andRespond(withServerError());

        assertThatThrownBy(() -> client.getRates(sampleRequest()))
                .isInstanceOfSatisfying(ShipTimeApiException.class,
                        e -> assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.UNAVAILABLE));
    }

    @Test
    void missingCredentialsFailBeforeCallingShipTime() {
        ShipTimeProperties empty = new ShipTimeProperties(BASE_URL, "", "", 1000, 1000);
        ShipTimeClient unconfigured = new ShipTimeClient(RestClient.create(), empty);

        assertThatThrownBy(() -> unconfigured.getRates(sampleRequest()))
                .isInstanceOfSatisfying(ShipTimeApiException.class,
                        e -> assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.UNAVAILABLE));
    }

    private static RateRequest sampleRequest() {
        Address from = new Address("John Smith", "Acme", "1 Main St", "Oakville", "ON", "L6J1H6", "CA", "905 555 1234");
        Address to = new Address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H2N2", "CA", "416 555 1234");
        LineItem item = new LineItem(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("2.5"));
        return new RateRequest(from, to, List.of(item), "PACKAGE", "METRIC", LocalDate.of(2026, 11, 12));
    }

    private static String sampleResponse() throws IOException {
        return new ClassPathResource("shiptime/rates-response.json").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
