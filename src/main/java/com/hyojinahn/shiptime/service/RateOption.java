package com.hyojinahn.shiptime.service;

import java.math.BigDecimal;

/** One carrier service shown to the user. {@code price} is in dollars, not cents. */
public record RateOption(
        String quoteId,
        String carrierId,
        String carrierName,
        String serviceId,
        String serviceName,
        BigDecimal price,
        String currency,
        Integer transitDays) {

    public String transitText() {
        if (transitDays == null) {
            return "-";
        }
        return transitDays + (transitDays == 1 ? " day" : " days");
    }
}
