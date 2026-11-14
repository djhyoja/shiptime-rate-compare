package com.hyojinahn.shiptime.client.dto;

import java.util.List;

/**
 * Response of {@code POST /rest/shipments}.
 */
public record ShipmentResponse(
        Long shipId,
        List<String> trackingNumbers,
        String labelUrl,
        String carrierTrackingUrl) {

    /** Never null, so templates can loop over it. */
    public List<String> trackingNumbers() {
        return trackingNumbers == null ? List.of() : trackingNumbers;
    }
}
