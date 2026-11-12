package com.hyojinahn.shiptime.client.dto;

import java.util.List;

public record RateResponse(List<Quote> availableRates, List<String> messages) {

    public static RateResponse empty() {
        return new RateResponse(List.of(), List.of());
    }

    public List<Quote> rates() {
        return availableRates == null ? List.of() : availableRates;
    }
}
