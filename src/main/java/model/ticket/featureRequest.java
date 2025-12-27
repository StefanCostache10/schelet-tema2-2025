package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class featureRequest extends Ticket {
    @JsonIgnore
    private String businessValue;
    @JsonIgnore// S, M, L, XL
    private String customerDemand; // LOW, MEDIUM, etc.

    public featureRequest() { super(); }

    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }

    public String getCustomerDemand() { return customerDemand; }
    public void setCustomerDemand(String customerDemand) { this.customerDemand = customerDemand; }
}