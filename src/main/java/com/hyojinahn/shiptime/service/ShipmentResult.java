package com.hyojinahn.shiptime.service;

import java.util.List;

/**
 * A created shipment and the rate it was created with.
 * The label is served by this app, so ShipTime's label URL is not kept.
 */
public record ShipmentResult(
        long shipId,
        List<String> trackingNumbers,
        String carrierTrackingUrl,
        RateOption option) {
}
