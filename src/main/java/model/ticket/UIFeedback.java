package model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class UIFeedback extends Ticket {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String uiElementId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String businessValue;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer usabilityScore;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String screenshotUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String suggestedFix;

    public UIFeedback() {
        super();
    }

    public String getUiElementId() {
        return uiElementId;
    }

    public void setUiElementId(final String uiElementId) {
        this.uiElementId = uiElementId;
    }

    public String getBusinessValue() {
        return businessValue;
    }

    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }

    public Integer getUsabilityScore() {
        return usabilityScore;
    }

    public void setUsabilityScore(final Integer usabilityScore) {
        this.usabilityScore = usabilityScore;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(final String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(final String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }
}
