package model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Milestone {
    private String name;
    private List<String> blockingFor;
    private String dueDate;
    private List<Integer> tickets;
    private List<String> assignedDevs;

    // Câmpuri necesare pentru trasabilitate
    private String createdAt;
    private String createdBy;

    public Milestone() {}

    // Getters and Setters (asigură-te că le ai pe toate cele de mai sus)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getBlockingFor() { return blockingFor; }
    public void setBlockingFor(List<String> blockingFor) { this.blockingFor = blockingFor; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public List<Integer> getTickets() { return tickets; }
    public void setTickets(List<Integer> tickets) { this.tickets = tickets; }
    public List<String> getAssignedDevs() { return assignedDevs; }
    public void setAssignedDevs(List<String> assignedDevs) { this.assignedDevs = assignedDevs; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}