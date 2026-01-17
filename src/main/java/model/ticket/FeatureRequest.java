package model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class FeatureRequest extends Ticket {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String businessValue;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String customerDemand;

    public FeatureRequest() {
        super();
    }

    public String getBusinessValue() {
        return businessValue;
    }

    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }

    public String getCustomerDemand() {
        return customerDemand;
    }

    public void setCustomerDemand(final String customerDemand) {
        this.customerDemand = customerDemand;
    }
}
