package com.hyojinahn.shiptime.client.dto;

/** One rate in the response. Fields not used by this app are ignored. */
public record Quote(
        String quoteId,
        String carrierId,
        String carrierName,
        String serviceId,
        String serviceName,
        Money totalCharge,
        Integer transitDays,
        String transitUnit) {
}
