package com.hyojinahn.shiptime.client.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Body of {@code POST /rest/rates}. Only the required fields are sent.
 */
public record RateRequest(
        Address from,
        Address to,
        List<LineItem> lineItems,
        String packageType,
        String unitOfMeasurement,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate shipDate) {
}
