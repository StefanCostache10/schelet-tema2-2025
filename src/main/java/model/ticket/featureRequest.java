package model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;

public class featureRequest extends Ticket {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String businessValue;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String customerDemand;

    public featureRequest() { super(); }

    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }

    public String getCustomerDemand() { return customerDemand; }
    public void setCustomerDemand(String customerDemand) { this.customerDemand = customerDemand; }
}