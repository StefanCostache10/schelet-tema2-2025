package model.ticket;

public class Bug extends Ticket {
    private String expectedBehavior;
    private String actualBehavior;
    private String frequency; // RARE, OCCASIONAL, etc.
    private String severity;  // MINOR, MODERATE, SEVERE
    private String environment;
    private Integer errorCode; // Integer pt a permite null

    public Bug() { super(); }

    // Getters/Setters standard...
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