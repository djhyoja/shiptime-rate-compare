package com.hyojinahn.shiptime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.service.RateOption;
import com.hyojinahn.shiptime.service.RateQuote;
import com.hyojinahn.shiptime.service.ShipmentResult;
import com.hyojinahn.shiptime.service.ShipmentService;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    private static final byte[] PDF = "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII);

    private static final RateOption CANPAR =
            new RateOption("q1", "14", "Canpar", "1", "Ground", new BigDecimal("12.75"), "CAD", 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentService shipmentService;

    @Test
    void createsShipmentAndShowsResult() throws Exception {
        RateQuote quote = sampleQuote();
        MockHttpSession session = sessionWith(quote);
        when(shipmentService.createShipment(eq(quote), eq("q1"))).thenReturn(sampleShipment());

        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipments/result"));

        mockMvc.perform(get("/shipments/result").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment"))
                .andExpect(content().string(containsString("4841164")))
                .andExpect(content().string(containsString("D420917200000007637001")))
                .andExpect(content().string(containsString("Canpar Ground")))
                .andExpect(content().string(containsString("href=\"/shipments/4841164/label\"")))
                .andExpect(content().string(not(containsString("sandboxrestapi.shiptime.com"))));
    }

    @Test
    void labelOfOwnShipmentIsServedAsPdf() throws Exception {
        MockHttpSession session = sessionWith(sampleQuote());
        when(shipmentService.createShipment(any(), any())).thenReturn(sampleShipment());
        when(shipmentService.getLabel(4841164L)).thenReturn(PDF);
        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session));

        mockMvc.perform(get("/shipments/4841164/label").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("label-4841164.pdf")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void labelOfShipmentFromAnotherSessionIsNotFound() throws Exception {
        mockMvc.perform(get("/shipments/4841164/label"))
                .andExpect(status().isNotFound());

        verify(shipmentService, never()).getLabel(anyLong());
    }

    @Test
    void labelFailureIsBadGateway() throws Exception {
        MockHttpSession session = sessionWith(sampleQuote());
        when(shipmentService.createShipment(any(), any())).thenReturn(sampleShipment());
        when(shipmentService.getLabel(4841164L)).thenThrow(ShipTimeApiException.unavailable("ShipTime returned 500", null));
        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session));

        mockMvc.perform(get("/shipments/4841164/label").session(session))
                .andExpect(status().isBadGateway());
    }

    @Test
    void quoteIsRemovedSoItCannotBeShippedTwice() throws Exception {
        MockHttpSession session = sessionWith(sampleQuote());
        when(shipmentService.createShipment(any(), any())).thenReturn(sampleShipment());

        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session));

        assertThat(session.getAttribute(RateController.SESSION_QUOTE)).isNull();
        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session))
                .andExpect(redirectedUrl("/"));
        verify(shipmentService).createShipment(any(), any());
    }

    @Test
    void unknownQuoteIdGoesBackToRates() throws Exception {
        MockHttpSession session = sessionWith(sampleQuote());
        when(shipmentService.createShipment(any(), eq("not-shown"))).thenReturn(Optional.empty());

        mockMvc.perform(post("/shipments").param("quoteId", "not-shown").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rates"));

        assertThat(session.getAttribute(RateController.SESSION_QUOTE)).isNotNull();
        assertThat(session.getAttribute(ShipmentController.SESSION_SHIPMENT)).isNull();
    }

    @Test
    void withoutQuoteInSessionGoesBackToForm() throws Exception {
        mockMvc.perform(post("/shipments").param("quoteId", "q1"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void rejectedShipmentShowsRatesAgainWithMessages() throws Exception {
        MockHttpSession session = sessionWith(sampleQuote());
        when(shipmentService.createShipment(any(), any()))
                .thenThrow(ShipTimeApiException.rejected(List.of("Quote has expired")));

        mockMvc.perform(post("/shipments").param("quoteId", "q1").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("rates"))
                .andExpect(content().string(containsString("could not create this shipment")))
                .andExpect(content().string(containsString("Quote has expired")));

        assertThat(session.getAttribute(RateController.SESSION_QUOTE)).isNotNull();
    }

    @Test
    void resultPageWithoutShipmentGoesBackToForm() throws Exception {
        mockMvc.perform(get("/shipments/result"))
                .andExpect(redirectedUrl("/"));
    }

    private static MockHttpSession sessionWith(RateQuote quote) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(RateController.SESSION_QUOTE, quote);
        return session;
    }

    private static RateQuote sampleQuote() {
        Address from = new Address("John Smith", "Acme", "1 Main St", "Oakville", "ON", "L6J1H6", "CA", "905 555 1234");
        Address to = new Address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H2N2", "CA", "416 555 1234");
        LineItem item = new LineItem(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("2.5"));
        RateRequest request = new RateRequest(from, to, List.of(item), "PACKAGE", "METRIC", LocalDate.of(2026, 11, 12));
        return new RateQuote(request, List.of(CANPAR));
    }

    private static Optional<ShipmentResult> sampleShipment() {
        return Optional.of(new ShipmentResult(
                4841164L,
                List.of("D420917200000007637001"),
                "https://carrier.test/track?ref=D420917200000007637001",
                CANPAR));
    }
}
