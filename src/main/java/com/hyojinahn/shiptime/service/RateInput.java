package com.hyojinahn.shiptime.service;

import java.math.BigDecimal;

/**
 * What the user wants to ship: two addresses and one package in centimetres and kilograms.
 */
public record RateInput(
        AddressInput from,
        AddressInput to,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        BigDecimal weight) {
}
