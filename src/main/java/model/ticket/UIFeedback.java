package model.ticket;

public class UIFeedback extends Ticket {
    private String uiElementId;
    private String businessValue;
    private Integer usabilityScore;
    private String screenshotUrl;
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