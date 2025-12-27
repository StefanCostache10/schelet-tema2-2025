package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UIFeedback extends Ticket {
    @JsonIgnore
    private String uiElementId;
    @JsonIgnore
    private String businessValue;
    @JsonIgnore
    private Integer usabilityScore;
    @JsonIgnore
    private String screenshotUrl;
    @JsonIgnore
    private String suggestedFix;

    public UIFeedback() { super(); }

    // Getters/Setters...
    public String getUiElementId() { return uiElementId; }
    public void setUiElementId(String uiElementId) { this.uiElementId = uiElementId; }

    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }

    public Integer getUsabilityScore() { return usabilityScore; }
    public void setUsabilityScore(Integer usabilityScore) { this.usabilityScore = usabilityScore; }

    public String getScreenshotUrl() { return screenshotUrl; }
    public void setScreenshotUrl(String screenshotUrl) { this.screenshotUrl = screenshotUrl; }

    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
}