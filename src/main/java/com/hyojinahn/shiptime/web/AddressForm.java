package com.hyojinahn.shiptime.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One Canadian address on the form. Limits follow the ShipTime API.
 * Input is trimmed and blank input becomes null before validation (see RateController).
 */
public class AddressForm {

    @NotNull(message = "Enter a contact name")
    @Size(min = 2, max = 40, message = "Use 2 to 40 characters")
    private String name;

    @NotBlank(message = "Enter a company name")
    @Size(max = 40, message = "Use up to 40 characters")
    private String company;

    @NotBlank(message = "Enter a street address")
    @Size(max = 50, message = "Use up to 50 characters")
    private String street;

    @NotBlank(message = "Enter a city")
    @Size(max = 40, message = "Use up to 40 characters")
    private String city;

    @NotNull(message = "Select a province")
    @Pattern(regexp = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT", message = "Select a province")
    private String province;

    @NotNull(message = "Enter a postal code")
    @Pattern(regexp = "\\s*[A-Za-z]\\d[A-Za-z] ?\\d[A-Za-z]\\d\\s*", message = "Use a postal code like L6J 1H6")
    private String postalCode;

    @NotNull(message = "Enter a phone number")
    @Pattern(regexp = "\\s*\\(?\\d{3}\\)?[ .-]?\\d{3}[ .-]?\\d{4}\\s*", message = "Use a phone number like 905 555 1234")
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
