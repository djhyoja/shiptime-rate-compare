package com.hyojinahn.shiptime.client.dto;

/** Amount in cents, for example 1881 = 18.81. */
public record Money(long amount, String currency) {
}
