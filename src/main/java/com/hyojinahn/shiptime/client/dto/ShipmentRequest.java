package com.hyojinahn.shiptime.client.dto;

/**
 * Body of {@code POST /rest/shipments}. The quote must come from a rate request
 * with exactly the same {@code rateRequest}.
 */
public record ShipmentRequest(
        String carrierId,
        String serviceId,
        String quoteId,
        RateRequest rateRequest) {
}
