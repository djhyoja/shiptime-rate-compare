package com.hyojinahn.shiptime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.Money;
import com.hyojinahn.shiptime.client.dto.Quote;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.RateResponse;
import com.hyojinahn.shiptime.web.AddressForm;
import com.hyojinahn.shiptime.web.RateForm;

class RateServiceTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    private ShipTimeClient client;
    private RateService service;

    @BeforeEach
    void setUp() {
        client = mock(ShipTimeClient.class);
        Clock fixed = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), TORONTO);
        service = new RateService(client, fixed);
    }

    @Test
    void buildsRequestFromFormWithFixedValues() {
        when(client.getRates(any())).thenReturn(RateResponse.empty());

        service.getRates(sampleForm());

        ArgumentCaptor<RateRequest> captor = ArgumentCaptor.forClass(RateRequest.class);
        verify(client).getRates(captor.capture());
        RateRequest request = captor.getValue();

        assertThat(request.packageType()).isEqualTo("PACKAGE");
        assertThat(request.unitOfMeasurement()).isEqualTo("METRIC");
        assertThat(request.shipDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(request.from().countryCode()).isEqualTo("CA");
        assertThat(request.from().postalCode()).isEqualTo("L6J1H6");
        assertThat(request.from().phone()).isEqualTo("905 555 1234");
        assertThat(request.from().attention()).isEqualTo("John Smith");
        assertThat(request.to().city()).isEqualTo("Toronto");
        assertThat(request.lineItems()).singleElement()
                .satisfies(item -> assertThat(item.weight()).isEqualByComparingTo("2.5"));
    }

    @Test
    void sortsOptionsByPriceAndConvertsCents() {
        when(client.getRates(any())).thenReturn(new RateResponse(List.of(
                quote("q-precision", "Precision", 1881, 1),
                quote("q-canpar", "Canpar", 1275, 1),
                quote("q-loomis", "Loomis", 1532, 1)), null));

        RateQuote result = service.getRates(sampleForm());

        assertThat(result.options()).extracting(RateOption::carrierName)
                .containsExactly("Canpar", "Loomis", "Precision");
        assertThat(result.options().get(0).price()).isEqualByComparingTo(new BigDecimal("12.75"));
        assertThat(result.options().get(0).currency()).isEqualTo("CAD");
    }

    @Test
    void samePriceShowsFasterServiceFirst() {
        when(client.getRates(any())).thenReturn(new RateResponse(List.of(
                quote("q-slow", "Slow", 1500, 3),
                quote("q-unknown", "Unknown", 1500, null),
                quote("q-fast", "Fast", 1500, 1)), null));

        RateQuote result = service.getRates(sampleForm());

        assertThat(result.options()).extracting(RateOption::carrierName)
                .containsExactly("Fast", "Slow", "Unknown");
    }

    @Test
    void skipsRatesWithoutTotal() {
        when(client.getRates(any())).thenReturn(new RateResponse(List.of(
                new Quote("q-broken", "1", "Broken", "S", "Service", null, 1, "Days"),
                quote("q-ok", "Ok", 1000, 1)), null));

        assertThat(service.getRates(sampleForm()).options()).extracting(RateOption::quoteId)
                .containsExactly("q-ok");
    }

    @Test
    void keepsRequestForTheShipmentStep() {
        when(client.getRates(any())).thenReturn(RateResponse.empty());

        RateQuote result = service.getRates(sampleForm());

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.request().to().postalCode()).isEqualTo("M5H2N2");
    }

    @Test
    void normalizesPhoneAndPostalCode() {
        assertThat(RateService.normalizePhone("(905) 555-1234")).isEqualTo("905 555 1234");
        assertThat(RateService.normalizePhone("905.555.1234")).isEqualTo("905 555 1234");
        assertThat(RateService.normalizePostalCode(" l6j 1h6 ")).isEqualTo("L6J1H6");
    }

    private static Quote quote(String quoteId, String carrier, long cents, Integer transitDays) {
        return new Quote(quoteId, "id-" + carrier, carrier, "svc", "Ground",
                new Money(cents, "CAD"), transitDays, "Days");
    }

    static RateForm sampleForm() {
        RateForm form = new RateForm();
        form.setFrom(address("John Smith ", "Acme", "1 Main St", "Oakville", "ON", "l6j 1h6", "(905) 555-1234"));
        form.setTo(address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H 2N2", "416 555 1234"));
        form.setLength(new BigDecimal("30"));
        form.setWidth(new BigDecimal("20"));
        form.setHeight(new BigDecimal("10"));
        form.setWeight(new BigDecimal("2.5"));
        return form;
    }

    private static AddressForm address(String name, String company, String street, String city,
                                       String province, String postalCode, String phone) {
        AddressForm address = new AddressForm();
        address.setName(name);
        address.setCompany(company);
        address.setStreet(street);
        address.setCity(city);
        address.setProvince(province);
        address.setPostalCode(postalCode);
        address.setPhone(phone);
        return address;
    }
}
