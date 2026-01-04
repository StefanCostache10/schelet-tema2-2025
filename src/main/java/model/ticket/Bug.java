package model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bug extends Ticket {

    // Folosim WRITE_ONLY: citim din JSON (Input), dar nu scriem în JSON (Output)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String expectedBehavior;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String actualBehavior;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String frequency;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String severity;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String environment;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer errorCode;

    public Bug() { super(); }

    // Getters and Setters
    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }

    public String getActualBehavior() { return actualBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
}