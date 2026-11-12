package com.hyojinahn.shiptime.web;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Input for a rate quote: two addresses and one package in centimetres and kilograms. */
public class RateForm {

    @Valid
    private AddressForm from = new AddressForm();

    @Valid
    private AddressForm to = new AddressForm();

    @NotNull(message = "Enter the length")
    @DecimalMin(value = "0", inclusive = false, message = "Must be greater than 0")
    @Digits(integer = 4, fraction = 2, message = "Use up to 2 decimal places")
    private BigDecimal length;

    @NotNull(message = "Enter the width")
    @DecimalMin(value = "0", inclusive = false, message = "Must be greater than 0")
    @Digits(integer = 4, fraction = 2, message = "Use up to 2 decimal places")
    private BigDecimal width;

    @NotNull(message = "Enter the height")
    @DecimalMin(value = "0", inclusive = false, message = "Must be greater than 0")
    @Digits(integer = 4, fraction = 2, message = "Use up to 2 decimal places")
    private BigDecimal height;

    @NotNull(message = "Enter the weight")
    @DecimalMin(value = "0", inclusive = false, message = "Must be greater than 0")
    @Digits(integer = 4, fraction = 3, message = "Use up to 3 decimal places")
    private BigDecimal weight;

    public AddressForm getFrom() { return from; }
    public void setFrom(AddressForm from) { this.from = from; }

    public AddressForm getTo() { return to; }
    public void setTo(AddressForm to) { this.to = to; }

    public BigDecimal getLength() { return length; }
    public void setLength(BigDecimal length) { this.length = length; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
