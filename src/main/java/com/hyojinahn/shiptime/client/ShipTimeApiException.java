package com.hyojinahn.shiptime.client;

import java.util.List;

/**
 * A ShipTime call failed. {@link Reason#REJECTED} means ShipTime refused the input
 * and {@link #details()} has its messages. {@link Reason#UNAVAILABLE} covers
 * authentication, server errors and network problems, which the user cannot fix.
 */
public class ShipTimeApiException extends RuntimeException {

    public enum Reason { REJECTED, UNAVAILABLE }

    private final Reason reason;
    private final List<String> details;

    public ShipTimeApiException(Reason reason, String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public static ShipTimeApiException rejected(List<String> details) {
        return new ShipTimeApiException(Reason.REJECTED, "ShipTime rejected the request", details, null);
    }

    public static ShipTimeApiException unavailable(String message, Throwable cause) {
        return new ShipTimeApiException(Reason.UNAVAILABLE, message, List.of(), cause);
    }

    public Reason reason() {
        return reason;
    }

    public List<String> details() {
        return details;
    }
}
