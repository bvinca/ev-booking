package com.smartcharge.evbooking.web.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class StationRequest {

    @NotBlank @Size(max = 140)
    private String name;

    @NotBlank @Size(max = 255)
    private String address;

    @NotBlank @Size(max = 100)
    private String city;

    @NotNull
    @DecimalMin(value = "-90.0")  @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @Size(max = 2000)
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
