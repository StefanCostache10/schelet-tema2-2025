package repository;

import model.Milestone;
import model.ticket.Ticket;
import model.user.User;
import model.enums.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Database {
    private static Database instance;
    private List<User> users = new ArrayList<>();
    private List<Ticket> tickets = new ArrayList<>();
    private List<Milestone> milestones = new ArrayList<>();
    private int ticketIdCounter = 0;
    private LocalDate appStartDate;
    private boolean appClosed = false;
    private LocalDate currentSystemDate;

    private Database() {}

    public static synchronized Database getInstance() {
        if (instance == null) instance = new Database();
        return instance;
    }

    // Metoda care lipsea și cauza eroarea
    public ticketPriority getCalculatedPriority(Ticket ticket, String currentTimestamp) {
        Milestone m = findMilestoneForTicket(ticket.getId());
        if (m == null) return ticket.getBusinessPriority();
        if (isMilestoneBlocked(m)) return ticket.getBusinessPriority();

        LocalDate now = LocalDate.parse(currentTimestamp);
        LocalDate created = LocalDate.parse(m.getCreatedAt());
        LocalDate due = LocalDate.parse(m.getDueDate());

        // Regula: Cu o zi înainte de dueDate devine CRITICAL
        if (now.isAfter(due.minusDays(2))) return ticketPriority.CRITICAL;

        // Regula: La fiecare 3 zile crește cu o treaptă
        long days = ChronoUnit.DAYS.between(created, now);
        int steps = (int) (days / 3);

        ticketPriority p = ticket.getBusinessPriority();
        for (int i = 0; i < steps; i++) p = p.next();

        // Regula complex_edge: Dacă depășește senioritatea devului, tichetul devine OPEN
        checkPrioritySeniorityConflict(ticket, p);

        return p;
    }
    public void updateCurrentDate(String timestampStr) {
        LocalDate newDate = LocalDate.parse(timestampStr);

        // Dacă este prima dată sau timpul a avansat
        if (currentSystemDate == null) {
            currentSystemDate = newDate;
            checkMilestoneDeadlines(currentSystemDate);
        } else if (newDate.isAfter(currentSystemDate)) {
            // Simulăm trecerea zilelor pentru a nu rata notificarea de "o zi înainte"
            // dacă sărim de la 18.11 la 21.11, trebuie să verificăm și 19, 20.
            LocalDate d = currentSystemDate.plusDays(1);
            while (!d.isAfter(newDate)) {
                checkMilestoneDeadlines(d);
                d = d.plusDays(1);
            }
            currentSystemDate = newDate;
        }
    }

    private void checkMilestoneDeadlines(LocalDate dateToCheck) {
        for (Milestone m : milestones) {
            // Dacă milestone-ul este terminat (toate tichetele closed), ignorăm (sau verificăm isMilestoneBlocked)
            if (isMilestoneFinished(m)) continue;

            // Dacă este blocat, regulile de deadline nu se aplică (conform enunț)
            if (isMilestoneBlocked(m)) continue;

            LocalDate due = LocalDate.parse(m.getDueDate());

            // Regula: O zi calendaristică înainte de dueDate
            // Exemplu: Due = 2025-11-20. O zi înainte = 2025-11-19.
            if (dateToCheck.equals(due.minusDays(1))) {
                String msg = "Milestone " + m.getName() + " is due tomorrow. All unresolved tickets are now CRITICAL.";
                notifyAssignedDevelopers(m, msg);
            }
        }
    }

    private void checkPrioritySeniorityConflict(Ticket ticket, ticketPriority calculated) {
        if (ticket.getStatus() == ticketStatus.IN_PROGRESS && !ticket.getAssignedTo().isEmpty()) {
            User dev = findUserByUsername(ticket.getAssignedTo());
            if (dev instanceof model.user.Developer) {
                // Dacă prioritatea calculată e mai mare decât ce poate duce dev-ul
                // (Implementare logică specifică în funcție de tabelul din enunț)
            }
        }
    }

    public boolean isMilestoneBlocked(Milestone m) {
        return milestones.stream().anyMatch(other ->
                other.getBlockingFor() != null &&
                        other.getBlockingFor().contains(m.getName()) &&
                        !isMilestoneFinished(other));
    }

    private boolean isMilestoneFinished(Milestone m) {
        return m.getTickets().stream()
                .map(this::findTicketById)
                .allMatch(t -> t != null && t.getStatus() == ticketStatus.CLOSED);
    }

    public void notifyUser(String username, String message) {
        User user = findUserByUsername(username);
        if (user != null) {
            user.update(message);
        }
    }

    public void notifyAssignedDevelopers(Milestone milestone, String message) {
        if (milestone.getAssignedDevs() != null) {
            for (String devUsername : milestone.getAssignedDevs()) {
                notifyUser(devUsername, message);
            }
        }
    }

    public void closeApp() { this.appClosed = true; }
    public boolean isAppClosed() { return appClosed; }

    // Gestiune date
    public void setUsers(List<User> users) { this.users = users; }
    public List<User> getUsers() { return users; }
    public List<Ticket> getTickets() { return tickets; }
    public List<Milestone> getMilestones() { return milestones; }
    public void addTicket(Ticket t) { t.setId(ticketIdCounter++); tickets.add(t); }
    public void addMilestone(Milestone m) { milestones.add(m); }
    public Ticket findTicketById(int id) { return tickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null); }
    public User findUserByUsername(String u) { return users.stream().filter(user -> user.getUsername().equals(u)).findFirst().orElse(null); }
    public Milestone findMilestoneForTicket(int id) { return milestones.stream().filter(m -> m.getTickets().contains(id)).findFirst().orElse(null); }
    public LocalDate getAppStartDate() { return appStartDate; }
    public void setAppStartDate(LocalDate d) { this.appStartDate = d; }
    public void reset() { users.clear(); tickets.clear(); milestones.clear(); ticketIdCounter = 0; appStartDate = null; appClosed = false; }
}