package com.hyojinahn.shiptime.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RateSortTest {

    private static final RateOption UBER = option("Uber", "5.00", 98);
    private static final RateOption CANPAR = option("Canpar", "12.75", 2);
    private static final RateOption NATIONEX = option("Nationex", "13.19", 1);
    private static final RateOption LOOMIS = option("Loomis", "15.16", 1);
    private static final RateOption UNKNOWN = option("Unknown", "9.00", null);

    private static final List<RateOption> ALL = List.of(LOOMIS, UBER, UNKNOWN, NATIONEX, CANPAR);

    @Test
    void priceSortsCheapestFirst() {
        assertThat(RateSort.PRICE.sort(ALL)).extracting(RateOption::carrierName)
                .containsExactly("Uber", "Unknown", "Canpar", "Nationex", "Loomis");
    }

    @Test
    void transitSortsFastestFirstThenCheapestAndUnknownLast() {
        assertThat(RateSort.TRANSIT.sort(ALL)).extracting(RateOption::carrierName)
                .containsExactly("Nationex", "Loomis", "Canpar", "Uber", "Unknown");
    }

    @Test
    void readsUrlValueAndFallsBackToPrice() {
        assertThat(RateSort.fromParam("transit")).isEqualTo(RateSort.TRANSIT);
        assertThat(RateSort.fromParam("TRANSIT")).isEqualTo(RateSort.TRANSIT);
        assertThat(RateSort.fromParam("price")).isEqualTo(RateSort.PRICE);
        assertThat(RateSort.fromParam("unknown")).isEqualTo(RateSort.PRICE);
        assertThat(RateSort.fromParam(null)).isEqualTo(RateSort.PRICE);
        assertThat(RateSort.TRANSIT.param()).isEqualTo("transit");
    }

    private static RateOption option(String carrier, String price, Integer transitDays) {
        return new RateOption("q-" + carrier, "id", carrier, "svc", "Ground", new BigDecimal(price), "CAD", transitDays);
    }
}
