package com.hyojinahn.shiptime.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.service.RateOption;
import com.hyojinahn.shiptime.service.RateQuote;
import com.hyojinahn.shiptime.service.RateService;

@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    @Test
    void showsEmptyForm() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Get rates")))
                .andExpect(content().string(containsString("From (origin)")));
    }

    @Test
    void invalidInputShowsErrorsAndDoesNotCallShipTime() throws Exception {
        mockMvc.perform(form(Map.of("from.postalCode", "12345", "weight", "")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("rateForm", "from.postalCode", "weight"))
                .andExpect(content().string(containsString("Use a postal code like L6J 1H6")))
                .andExpect(content().string(containsString("Enter the weight")));

        verify(rateService, never()).getRates(any());
    }

    @Test
    void validInputRedirectsToRatesAndShowsThem() throws Exception {
        when(rateService.getRates(any())).thenReturn(sampleQuote());
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(validForm().session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rates"));

        mockMvc.perform(get("/rates").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("rates"))
                .andExpect(content().string(containsString("Canpar")))
                .andExpect(content().string(containsString("$12.75 CAD")))
                .andExpect(content().string(containsString("Oakville, ON")));
    }

    @Test
    void ratesPageWithoutQuoteGoesBackToForm() throws Exception {
        mockMvc.perform(get("/rates"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void noRatesShowsMessageOnForm() throws Exception {
        when(rateService.getRates(any())).thenReturn(new RateQuote(sampleRequest(), List.of()));

        mockMvc.perform(validForm())
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("No rates found")));
    }

    @Test
    void rejectedRequestShowsShipTimeMessages() throws Exception {
        when(rateService.getRates(any())).thenThrow(ShipTimeApiException.rejected(List.of("Invalid postal code")));

        mockMvc.perform(validForm())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("could not accept these details")))
                .andExpect(content().string(containsString("Invalid postal code")));
    }

    @Test
    void unavailableServiceShowsGenericMessage() throws Exception {
        when(rateService.getRates(any())).thenThrow(ShipTimeApiException.unavailable("ShipTime returned 500", null));

        mockMvc.perform(validForm())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("not available right now")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("500"))));
    }

    private static MockHttpServletRequestBuilder validForm() {
        return form(Map.of());
    }

    /** A valid form, with some fields replaced by {@code overrides}. */
    private static MockHttpServletRequestBuilder form(Map<String, String> overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("from.name", "John Smith");
        fields.put("from.company", "Acme");
        fields.put("from.street", "1 Main St");
        fields.put("from.city", "Oakville");
        fields.put("from.province", "ON");
        fields.put("from.postalCode", "L6J 1H6");
        fields.put("from.phone", "905 555 1234");
        fields.put("to.name", "Jane Doe");
        fields.put("to.company", "Beta");
        fields.put("to.street", "100 Queen St W");
        fields.put("to.city", "Toronto");
        fields.put("to.province", "ON");
        fields.put("to.postalCode", "M5H 2N2");
        fields.put("to.phone", "416 555 1234");
        fields.put("length", "30");
        fields.put("width", "20");
        fields.put("height", "10");
        fields.put("weight", "2.5");
        fields.putAll(overrides);

        MockHttpServletRequestBuilder request = post("/rates");
        fields.forEach(request::param);
        return request;
    }

    private static RateRequest sampleRequest() {
        Address from = new Address("John Smith", "Acme", "1 Main St", "Oakville", "ON", "L6J1H6", "CA", "905 555 1234");
        Address to = new Address("Jane Doe", "Beta", "100 Queen St W", "Toronto", "ON", "M5H2N2", "CA", "416 555 1234");
        LineItem item = new LineItem(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("2.5"));
        return new RateRequest(from, to, List.of(item), "PACKAGE", "METRIC", LocalDate.of(2026, 11, 12));
    }

    private static RateQuote sampleQuote() {
        RateOption canpar = new RateOption("q1", "14", "Canpar", "1", "Ground", new BigDecimal("12.75"), "CAD", 1);
        return new RateQuote(sampleRequest(), List.of(canpar));
    }
}
