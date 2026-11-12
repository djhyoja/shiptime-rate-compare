package com.hyojinahn.shiptime.client.dto;

import java.math.BigDecimal;

public record LineItem(BigDecimal length, BigDecimal width, BigDecimal height, BigDecimal weight) {
}
