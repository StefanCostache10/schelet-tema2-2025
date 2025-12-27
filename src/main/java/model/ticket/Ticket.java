package model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import model.enums.ticketPriority;
import model.enums.ticketStatus;
import model.enums.ticketType;

// Ignorăm câmpurile care sunt în JSON-ul comenzii dar nu aparțin tichetului (ex: "command")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Ticket {
    private int id;
    private ticketType type;
    private String title;
    private String description;
    private ticketPriority businessPriority;
    private ticketStatus status = ticketStatus.OPEN; // Default
    private String expertiseArea;
    private String reportedBy; // Username-ul reporterului

    // Câmpuri de sistem (nu vin neapărat din JSON-ul de create)
    private String assignedTo; // Username developer
    private String timestamp;  // Data creării/raportării

    public Ticket() {}

    // Getters and Setters
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
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    // ... în interiorul clasei Ticket
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
// ...
}