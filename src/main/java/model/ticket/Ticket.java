// model/ticket/Ticket.java
package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.enums.ticketPriority;
import model.enums.ticketStatus;
import model.enums.ticketType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Ticket {
    private int id;
    private ticketType type;
    private String title;

    @JsonIgnore // Nu apare în viewTickets conform ref
    private String description;

    private ticketPriority businessPriority;
    private ticketStatus status = ticketStatus.OPEN;

    @JsonIgnore // Nu apare în viewTickets conform ref
    private String expertiseArea;

    private String reportedBy = "";
    private String assignedTo = ""; // Inițializat cu șir gol conform ref

    // Câmpuri noi cerute de referință
    @JsonProperty("createdAt")
    private String timestamp; // Mapăm timestamp-ul intern la cheia "createdAt"

    private String assignedAt = ""; // Default empty string
    private String solvedAt = "";   // Default empty string
    private List<Object> comments = new ArrayList<>(); // Listă goală

    public Ticket() {}

    // Getters și Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public ticketType getType() { return type; }
    public void setType(ticketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ticketPriority getBusinessPriority() { return businessPriority; }
    public void setBusinessPriority(ticketPriority businessPriority) { this.businessPriority = businessPriority; }
    public ticketStatus getStatus() { return status; }
    public void setStatus(ticketStatus status) { this.status = status; }
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
    public List<Object> getComments() { return comments; }
    public void setComments(List<Object> comments) { this.comments = comments; }
}