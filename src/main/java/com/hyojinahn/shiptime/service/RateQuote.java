package com.hyojinahn.shiptime.service;

import java.util.List;

import com.hyojinahn.shiptime.client.dto.RateRequest;

/**
 * The request sent to ShipTime and the rates it returned.
 * Kept in the HTTP session because creating a shipment needs both.
 */
public record RateQuote(RateRequest request, List<RateOption> options) {

    public boolean isEmpty() {
        return options.isEmpty();
    }
}
