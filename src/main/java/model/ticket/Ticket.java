package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.ticketPriority;
import model.enums.ticketStatus;
import model.enums.ticketType;

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
        @JsonSubTypes.Type(value = featureRequest.class, name = "FEATURE_REQUEST"),
        @JsonSubTypes.Type(value = UIFeedback.class, name = "UI_FEEDBACK")
})
public abstract class Ticket {
    private int id; // Identificator unic
    private ticketType type; // BUG, FEATURE_REQUEST, UI_FEEDBACK
    private String title; // Titlu scurt

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String description; // Descriere (vizibilă doar la citire)

    private ticketPriority businessPriority; // LOW, MEDIUM, HIGH, CRITICAL
    private ticketStatus status = ticketStatus.OPEN; // Status inițial

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String expertiseArea; // Expertiză necesară (vizibilă doar la citire)

    private String reportedBy = ""; // Persoana care a inițiat tichetul
    private String assignedTo = ""; // Developerul responsabil

    @JsonProperty("createdAt")
    private String timestamp; // Mapare timestamp -> createdAt pentru output

    private String assignedAt = ""; // Data la care a fost preluat
    private String solvedAt = "";   // Data la care a fost rezolvat

    // Câmp intern pentru logica de Milestone - Nu apare în JSON!
    private String closedAt = "";

    // Lista de comentarii
    private List<Comment> comments = new ArrayList<>();

    // Istoricul acțiunilor - Ignorat la serializarea JSON standard
    @JsonIgnore
    private List<ObjectNode> actions = new ArrayList<>();

    public Ticket() {}

    // --- Getters și Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public ticketType getType() { return type; }
    public void setType(ticketType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ticketPriority getBusinessPriority() { return businessPriority; }
    public void setBusinessPriority(ticketPriority businessPriority) { this.businessPriority = businessPriority; }

    public ticketStatus getStatus() { return status; }
    public void setStatus(ticketStatus status) { this.status = status; }

    public String getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(String expertiseArea) { this.expertiseArea = expertiseArea; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = (assignedTo == null) ? "" : assignedTo; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

    public String getSolvedAt() { return solvedAt; }
    public void setSolvedAt(String solvedAt) { this.solvedAt = solvedAt; }

    // Adăugăm @JsonIgnore aici pentru a ascunde câmpul din output-ul JSON
    @JsonIgnore
    public String getClosedAt() { return closedAt; }
    public void setClosedAt(String closedAt) { this.closedAt = closedAt; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    @JsonIgnore
    public List<ObjectNode> getActions() { return actions; }

    public void addAction(ObjectNode action) { this.actions.add(action); }
}