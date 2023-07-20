package com.explore.dileepkumar.cdc.customer;

public enum CustomerType {
    Basic("Basic"), CLASSIC("Classic"), PLATINUM("Platinum");

    private final String value;

    private CustomerType(String value) {
        this.value = value;
    }
}
