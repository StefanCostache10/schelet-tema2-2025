package model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Bug extends Ticket {

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

    public Bug() {
        super();
    }

    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    public void setExpectedBehavior(final String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    public String getActualBehavior() {
        return actualBehavior;
    }

    public void setActualBehavior(final String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(final String environment) {
        this.environment = environment;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final Integer errorCode) {
        this.errorCode = errorCode;
    }
}
