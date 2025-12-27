package repository;

import model.Milestone;
import model.ticket.Ticket;
import model.user.User;

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
}