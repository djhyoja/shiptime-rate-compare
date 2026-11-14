package com.hyojinahn.shiptime.service;

import org.springframework.stereotype.Service;

import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentRequest;
import com.hyojinahn.shiptime.client.dto.ShipmentResponse;

/**
 * Creates a shipment for the rate the user picked.
 */
@Service
public class ShipmentService {

    private final ShipTimeClient client;

    public ShipmentService(ShipTimeClient client) {
        this.client = client;
    }

    /**
     * Sends the same rate request that produced the quote, together with the chosen option.
     */
    public ShipmentResponse createShipment(RateRequest rateRequest, RateOption option) {
        ShipmentRequest request = new ShipmentRequest(
                option.carrierId(),
                option.serviceId(),
                option.quoteId(),
                rateRequest);
        return client.createShipment(request);
    }
}
