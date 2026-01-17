package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.TicketPriority;
import model.enums.TicketStatus;
import model.enums.TicketType;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasa de bază pentru toate tipurile de tichete din sistem.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Bug.class, name = "BUG"),
        @JsonSubTypes.Type(value = FeatureRequest.class, name = "FEATURE_REQUEST"),
        @JsonSubTypes.Type(value = UIFeedback.class, name = "UI_FEEDBACK")
})
public abstract class Ticket {
    private int id;
    private TicketType type;
    private String title;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String description;

    private TicketPriority businessPriority;
    private TicketStatus status = TicketStatus.OPEN;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String expertiseArea;

    private String reportedBy = "";
    private String assignedTo = "";

    @JsonProperty("createdAt")
    private String timestamp;

    private String assignedAt = "";
    private String solvedAt = "";

    private String closedAt = "";

    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore
    private List<ObjectNode> actions = new ArrayList<>();

    public Ticket() {
    }

    public final int getId() {
        return id;
    }

    public final void setId(final int id) {
        this.id = id;
    }

    public final TicketType getType() {
        return type;
    }

    public final void setType(final TicketType type) {
        this.type = type;
    }

    public final String getTitle() {
        return title;
    }

    public final void setTitle(final String title) {
        this.title = title;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(final String description) {
        this.description = description;
    }

    public final TicketPriority getBusinessPriority() {
        return businessPriority;
    }

    public final void setBusinessPriority(final TicketPriority businessPriority) {
        this.businessPriority = businessPriority;
    }

    public final TicketStatus getStatus() {
        return status;
    }

    public final void setStatus(final TicketStatus status) {
        this.status = status;
    }

    public final String getExpertiseArea() {
        return expertiseArea;
    }

    public final void setExpertiseArea(final String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    public final String getReportedBy() {
        return reportedBy;
    }

    public final void setReportedBy(final String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public final String getAssignedTo() {
        return assignedTo;
    }

    public final void setAssignedTo(final String assignedTo) {
        this.assignedTo = (assignedTo == null) ? "" : assignedTo;
    }

    public final String getTimestamp() {
        return timestamp;
    }

    public final void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public final String getAssignedAt() {
        return assignedAt;
    }

    public final void setAssignedAt(final String assignedAt) {
        this.assignedAt = assignedAt;
    }

    public final String getSolvedAt() {
        return solvedAt;
    }

    public final void setSolvedAt(final String solvedAt) {
        this.solvedAt = solvedAt;
    }

    @JsonIgnore
    public final String getClosedAt() {
        return closedAt;
    }

    public final void setClosedAt(final String closedAt) {
        this.closedAt = closedAt;
    }

    public final List<Comment> getComments() {
        return comments;
    }

    public final void setComments(final List<Comment> comments) {
        this.comments = comments;
    }

    @JsonIgnore
    public final List<ObjectNode> getActions() {
        return actions;
    }

    /**
     * Adds an action to the history of this ticket.
     *
     * @param action The action to add.
     */
    public final void addAction(final ObjectNode action) {
        this.actions.add(action);
    }
}
