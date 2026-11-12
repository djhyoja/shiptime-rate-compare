package com.hyojinahn.shiptime.client.dto;

public record Address(
        String attention,
        String companyName,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String countryCode,
        String phone) {
}
