package com.hyojinahn.shiptime.service;

/**
 * One Canadian address as the service needs it. The values are already validated by the caller.
 */
public record AddressInput(
        String name,
        String company,
        String street,
        String city,
        String province,
        String postalCode,
        String phone) {
}
