package com.hyojinahn.shiptime.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.Quote;
import com.hyojinahn.shiptime.client.dto.RateRequest;

/**
 * Builds the rate request from the form, calls ShipTime and returns the
 * options sorted from cheapest to most expensive.
 */
@Service
public class RateService {

    static final String PACKAGE_TYPE = "PACKAGE";
    static final String UNIT_OF_MEASUREMENT = "METRIC";
    static final String COUNTRY_CODE = "CA";

    private final ShipTimeClient client;
    private final Clock clock;

    public RateService(ShipTimeClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public RateQuote getRates(RateInput input) {
        RateRequest request = toRequest(input);
        List<RateOption> options = client.getRates(request).rates().stream()
                .filter(quote -> quote.totalCharge() != null) // drop null price
                .map(RateService::toOption) // cent to dollar
                .toList();
        return new RateQuote(request, RateSort.PRICE.sort(options));
    }

    RateRequest toRequest(RateInput input) {
        LineItem item = new LineItem(input.length(), input.width(), input.height(), input.weight());
        return new RateRequest(
                toAddress(input.from()),
                toAddress(input.to()),
                List.of(item),
                PACKAGE_TYPE,
                UNIT_OF_MEASUREMENT,
                LocalDate.now(clock));
    }

    private static Address toAddress(AddressInput input) {
        return new Address(	
                input.name().trim(),
                input.company().trim(),
                input.street().trim(),
                input.city().trim(),
                input.province(),
                normalizePostalCode(input.postalCode()),
                COUNTRY_CODE,
                normalizePhone(input.phone()));
    }

    /** "l6j 1h6" → "L6J1H6" */
    static String normalizePostalCode(String postalCode) {
        return postalCode.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    /** "(905) 555-1234" → "905 555 1234", the format ShipTime asks for. */
    static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    private static RateOption toOption(Quote quote) {
        BigDecimal price = BigDecimal.valueOf(quote.totalCharge().amount(), 2);
        return new RateOption(
                quote.quoteId(),
                quote.carrierId(),
                quote.carrierName(),
                quote.serviceId(),
                quote.serviceName(),
                price,
                quote.totalCharge().currency(),
                quote.transitDays());
    }
}
