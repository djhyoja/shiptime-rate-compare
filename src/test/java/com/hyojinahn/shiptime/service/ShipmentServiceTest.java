package com.hyojinahn.shiptime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;

class ShipmentServiceTest {

    private final ShipTimeClient client = mock(ShipTimeClient.class);
    private final ShipmentService service = new ShipmentService(client);

    @Test
    void sendsChosenOptionWithTheOriginalRateRequest() {
        RateRequest rateRequest = sampleRequest();
        RateOption canpar = new RateOption("q1", "14", "Canpar", "1", "Ground", new BigDecimal("12.75"), "CAD", 2);
        ShipmentResponse created = new ShipmentResponse(4841164L, List.of("T1"), "http://label", "https://track");
        when(client.createShipment(any())).thenReturn(created);

        ShipmentResponse result = service.createShipment(rateRequest, canpar);

        ArgumentCaptor<ShipmentRequest> captor = ArgumentCaptor.forClass(ShipmentRequest.class);
        verify(client).createShipment(captor.capture());
        ShipmentRequest sent = captor.getValue();
        assertThat(sent.carrierId()).isEqualTo("14");
        assertThat(sent.serviceId()).isEqualTo("1");
        assertThat(sent.quoteId()).isEqualTo("q1");
        assertThat(sent.rateRequest()).isSameAs(rateRequest);
        assertThat(result).isSameAs(created);
    }

    private static RateRequest sampleRequest() {
        Address from = new Address("John Smith", "Acme", "1 Main St", "Oakville", "ON", "L6J1H6", "CA", "905 555 1234");
        Address to = new Address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H2N2", "CA", "416 555 1234");
        LineItem item = new LineItem(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("2.5"));
        return new RateRequest(from, to, List.of(item), "PACKAGE", "METRIC", LocalDate.of(2026, 11, 12));
    }
}
