package model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Milestone {
    private String name;
    private List<String> blockingFor; // Numele altor milestone-uri
    private String dueDate; // Format YYYY-MM-DD
    private List<Integer> tickets; // Lista de ID-uri tichete
    private List<String> assignedDevs; // Username-uri

    // Câmpuri calculate/de stare
    private Integer assignedProjectManagerId; // Sau username-ul managerului creator

    public Milestone() {}

    // Getters and Setters
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
}