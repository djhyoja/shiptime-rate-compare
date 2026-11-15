package com.hyojinahn.shiptime.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hyojinahn.shiptime.client.ShipTimeApiException;
import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.ShipmentRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;

/**
 * Creates a shipment for the rate the user picked, and gets its label.
 */
@Service
public class ShipmentService {

    private final ShipTimeClient client;

    public ShipmentService(ShipTimeClient client) {
        this.client = client;
    }

    /**
     * Creates a shipment for one of the options in the quote. The same rate request
     * that produced the quote is sent again, because ShipTime checks that they match.
     *
     * @return empty if {@code quoteId} is not one of the options shown to the user
     */
    public Optional<ShipmentResult> createShipment(RateQuote quote, String quoteId) {
        Optional<RateOption> selected = quote.option(quoteId);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        RateOption option = selected.get();
        ShipmentRequest request = new ShipmentRequest(
                option.carrierId(),
                option.serviceId(),
                option.quoteId(),
                quote.request());
        return Optional.of(toResult(client.createShipment(request), option));
    }

    /** The label PDF of a shipment. */
    public byte[] getLabel(long shipId) {
        return client.getLabel(shipId);
    }

    private static ShipmentResult toResult(ShipmentResponse response, RateOption option) {
        if (response.shipId() == null) {
            throw ShipTimeApiException.unavailable("ShipTime did not return a shipment ID", null);
        }
        return new ShipmentResult(
                response.shipId(),
                response.trackingNumbers(),
                response.carrierTrackingUrl(),
                option);
    }
}
