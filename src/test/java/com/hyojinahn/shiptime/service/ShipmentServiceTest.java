package com.hyojinahn.shiptime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;

class ShipmentServiceTest {

    private static final RateOption CANPAR =
            new RateOption("q1", "14", "Canpar", "1", "Ground", new BigDecimal("12.75"), "CAD", 2);

    private final ShipTimeClient client = mock(ShipTimeClient.class);
    private final ShipmentService service = new ShipmentService(client);

    @Test
    void sendsChosenOptionWithTheOriginalRateRequest() {
        RateQuote quote = sampleQuote();
        when(client.createShipment(any())).thenReturn(new ShipmentResponse(
                4841164L, List.of("T1"), "http://label", "https://track"));

        Optional<ShipmentResult> result = service.createShipment(quote, "q1");

        ArgumentCaptor<ShipmentRequest> captor = ArgumentCaptor.forClass(ShipmentRequest.class);
        verify(client).createShipment(captor.capture());
        ShipmentRequest sent = captor.getValue();
        assertThat(sent.carrierId()).isEqualTo("14");
        assertThat(sent.serviceId()).isEqualTo("1");
        assertThat(sent.quoteId()).isEqualTo("q1");
        assertThat(sent.rateRequest()).isSameAs(quote.request());

        assertThat(result).hasValueSatisfying(shipment -> {
            assertThat(shipment.shipId()).isEqualTo(4841164L);
            assertThat(shipment.trackingNumbers()).containsExactly("T1");
            assertThat(shipment.carrierTrackingUrl()).isEqualTo("https://track");
            assertThat(shipment.option()).isEqualTo(CANPAR);
        });
    }

    @Test
    void quoteIdThatWasNotShownCreatesNothing() {
        assertThat(service.createShipment(sampleQuote(), "not-shown")).isEmpty();
        assertThat(service.createShipment(sampleQuote(), null)).isEmpty();

        verify(client, never()).createShipment(any());
    }

    @Test
    void responseWithoutShipmentIdIsAnError() {
        when(client.createShipment(any())).thenReturn(new ShipmentResponse(null, null, null, null));

        assertThatThrownBy(() -> service.createShipment(sampleQuote(), "q1"))
                .isInstanceOfSatisfying(ShipTimeApiException.class,
                        e -> assertThat(e.reason()).isEqualTo(ShipTimeApiException.Reason.UNAVAILABLE));
    }

    private static RateQuote sampleQuote() {
        Address from = new Address("John Smith", "Acme", "1 Main St", "Oakville", "ON", "L6J1H6", "CA", "905 555 1234");
        Address to = new Address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H2N2", "CA", "416 555 1234");
        LineItem item = new LineItem(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("2.5"));
        RateRequest request = new RateRequest(from, to, List.of(item), "PACKAGE", "METRIC", LocalDate.of(2026, 11, 12));
        return new RateQuote(request, List.of(CANPAR));
    }
}
