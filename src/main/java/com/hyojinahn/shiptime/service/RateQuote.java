package com.hyojinahn.shiptime.service;

import java.util.List;
import java.util.Optional;

import com.hyojinahn.shiptime.client.dto.RateRequest;

/**
 * The request sent to ShipTime and the rates it returned.
 * Kept in the HTTP session because creating a shipment needs both.
 */
public record RateQuote(RateRequest request, List<RateOption> options) {

    public boolean isEmpty() {
        return options.isEmpty();
    }

    /** The option with this quote ID, if it is one of the rates shown to the user. */
    public Optional<RateOption> option(String quoteId) {
        return options.stream()
                .filter(option -> option.quoteId().equals(quoteId))
                .findFirst();
    }
}
