package com.hyojinahn.shiptime.client.dto;

import java.util.List;

/** Error body returned by ShipTime with a 400 response. */
public record ErrorBody(List<String> messages) {
}
