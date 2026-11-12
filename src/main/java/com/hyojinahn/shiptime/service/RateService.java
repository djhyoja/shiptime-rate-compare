package com.hyojinahn.shiptime.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.hyojinahn.shiptime.client.ShipTimeClient;
import com.hyojinahn.shiptime.client.dto.Address;
import com.hyojinahn.shiptime.client.dto.LineItem;
import com.hyojinahn.shiptime.client.dto.Quote;
import com.hyojinahn.shiptime.client.dto.RateRequest;
import com.hyojinahn.shiptime.web.AddressForm;
import com.hyojinahn.shiptime.web.RateForm;

/**
 * Builds the rate request from the form, calls ShipTime and returns the
 * options sorted from cheapest to most expensive.
 */
@Service
public class RateService {

    static final String PACKAGE_TYPE = "PACKAGE";
    static final String UNIT_OF_MEASUREMENT = "METRIC";
    static final String COUNTRY_CODE = "CA";

    /** Cheapest first; same price → faster first; unknown transit time last. */
    private static final Comparator<RateOption> BY_PRICE = Comparator
            .comparing(RateOption::price)
            .thenComparing(RateOption::transitDays, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ShipTimeClient client;
    private final Clock clock;

    public RateService(ShipTimeClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    public RateQuote getRates(RateForm form) {
        RateRequest request = toRequest(form);
        List<RateOption> options = client.getRates(request).rates().stream()
                .filter(quote -> quote.totalCharge() != null) // drop null price
                .map(RateService::toOption) // cent to dollar
                .sorted(BY_PRICE) // sorting
                .toList();
        return new RateQuote(request, options);
    }

    RateRequest toRequest(RateForm form) {
        LineItem item = new LineItem(form.getLength(), form.getWidth(), form.getHeight(), form.getWeight());
        return new RateRequest(
                toAddress(form.getFrom()),
                toAddress(form.getTo()),
                List.of(item),
                PACKAGE_TYPE,
                UNIT_OF_MEASUREMENT,
                LocalDate.now(clock));
    }

    private static Address toAddress(AddressForm form) {
        return new Address(
                form.getName().trim(),
                form.getCompany().trim(),
                form.getStreet().trim(),
                form.getCity().trim(),
                form.getProvince(),
                normalizePostalCode(form.getPostalCode()),
                COUNTRY_CODE,
                normalizePhone(form.getPhone()));
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
