package model.ticket;

public class featureRequest extends Ticket {
    private String businessValue; // S, M, L, XL
    private String customerDemand; // LOW, MEDIUM, etc.

    public featureRequest() { super(); }

    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }

    public String getCustomerDemand() { return customerDemand; }
    public void setCustomerDemand(String customerDemand) { this.customerDemand = customerDemand; }
}