package repository;

import model.Milestone;
import model.ticket.Ticket;
import model.user.User;
import model.enums.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static Database instance;

    private List<User> users;
    private List<Ticket> tickets;
    private List<Milestone> milestones;
    private int ticketIdCounter = 0;

    // NOU: Data de start a aplicației pentru a calcula perioada de testare
    private LocalDate appStartDate;

    private Database() {
        this.users = new ArrayList<>();
        this.tickets = new ArrayList<>();
        this.milestones = new ArrayList<>();
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public User findUserByUsername(String username) {
        if (users == null) return null;
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public void addTicket(Ticket ticket) {
        ticket.setId(ticketIdCounter++);
        tickets.add(ticket);
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void addMilestone(Milestone milestone) {
        this.milestones.add(milestone);
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public String getMilestoneNameForTicket(int ticketId) {
        for (Milestone m : milestones) {
            if (m.getTickets() != null && m.getTickets().contains(ticketId)) {
                return m.getName();
            }
        }
        return null;
    }

    // Metode pentru AppStartDate
    public LocalDate getAppStartDate() {
        return appStartDate;
    }

    public void setAppStartDate(LocalDate appStartDate) {
        this.appStartDate = appStartDate;
    }

    public void reset() {
        this.users.clear();
        this.tickets.clear();
        this.milestones.clear();
        this.ticketIdCounter = 0;
        this.appStartDate = null; // Resetăm și data
    }

    // repository/Database.java
// Adăugăm o metodă pentru a obține prioritatea calculată
    public ticketPriority getCalculatedPriority(Ticket ticket, String currentTimestamp) {
        Milestone m = findMilestoneForTicket(ticket.getId());
        if (m == null) return ticket.getBusinessPriority();

        // Dacă milestone-ul este blocat, prioritatea nu crește
        if (isMilestoneBlocked(m)) return ticket.getBusinessPriority();

        LocalDate now = LocalDate.parse(currentTimestamp);
        LocalDate created = LocalDate.parse(m.getCreatedAt());
        LocalDate due = LocalDate.parse(m.getDueDate());

        // Regula: Cu o zi înainte de dueDate devine CRITICAL
        if (now.isAfter(due.minusDays(2))) { // due-1 sau mai târziu
            return ticketPriority.CRITICAL;
        }

        // Regula: La fiecare 3 zile crește cu o treaptă
        long days = java.time.temporal.ChronoUnit.DAYS.between(created, now);
        int steps = (int) (days / 3);

        ticketPriority p = ticket.getBusinessPriority();
        for (int i = 0; i < steps; i++) {
            p = p.next();
        }
        return p;
    }

    public boolean isMilestoneBlocked(Milestone m) {
        return milestones.stream().anyMatch(other ->
                other.getBlockingFor() != null &&
                        other.getBlockingFor().contains(m.getName()) &&
                        !isMilestoneFinished(other)
        );
    }

    private boolean isMilestoneFinished(Milestone m) {
        return m.getTickets().stream()
                .map(this::findTicketById)
                .allMatch(t -> t != null && t.getStatus() == ticketStatus.CLOSED);
    }

    public Ticket findTicketById(int id) {
        return tickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    public Milestone findMilestoneForTicket(int ticketId) {
        return milestones.stream()
                .filter(m -> m.getTickets().contains(ticketId))
                .findFirst().orElse(null);
    }
}