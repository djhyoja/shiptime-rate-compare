package com.hyojinahn.shiptime.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * How the rate options are ordered on the page.
 * Options without transit days always come last when sorting by transit time.
 */
public enum RateSort {

    /** Cheapest first; same price → faster first. */
    PRICE(Comparator
            .comparing(RateOption::price)
            .thenComparing(RateOption::transitDays, Comparator.nullsLast(Comparator.naturalOrder()))),

    /** Fastest first; same transit time → cheaper first. */
    TRANSIT(Comparator
            .comparing(RateOption::transitDays, Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
            .thenComparing(RateOption::price));

    private final Comparator<RateOption> comparator;

    RateSort(Comparator<RateOption> comparator) {
        this.comparator = comparator;
    }

    public List<RateOption> sort(List<RateOption> options) {
        return options.stream().sorted(comparator).toList();
    }

    /** The value used in the page URL, for example {@code ?sort=transit}. */
    public String param() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Unknown or missing values fall back to {@link #PRICE}. */
    public static RateSort fromParam(String value) {
        for (RateSort sort : values()) {
            if (sort.param().equalsIgnoreCase(value)) {
                return sort;
            }
        }
        return PRICE;
    }
}
