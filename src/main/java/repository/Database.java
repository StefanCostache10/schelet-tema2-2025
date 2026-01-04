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
    private java.time.LocalDate currentTestingPhaseStart;

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
// În Database.java

    public void updateCurrentDate(String timestampStr) {
        LocalDate newDate = LocalDate.parse(timestampStr);

        // Caz 1: Inițializare
        if (currentSystemDate == null) {
            currentSystemDate = newDate;
            // Check inițial (poate inputul începe direct cu o zi înainte de deadline)
            checkMilestoneDeadlines(currentSystemDate);
            return;
        }

        // Caz 2: Trecerea timpului (poate sări mai multe zile, ex: de pe 18 pe 21)
        // Trebuie să verificăm fiecare zi intermediară pentru a prinde momentul "due tomorrow"
        while (currentSystemDate.isBefore(newDate)) {
            currentSystemDate = currentSystemDate.plusDays(1);
            checkMilestoneDeadlines(currentSystemDate);
        }
    }

    public void startNewTestingPhase(String timestamp) {
        this.currentTestingPhaseStart = java.time.LocalDate.parse(timestamp);
    }

    public boolean isInTestingPhase(String currentTimestampStr) {
        java.time.LocalDate current = java.time.LocalDate.parse(currentTimestampStr);

        // Verificăm perioada inițială (de la startul aplicației)
        if (appStartDate != null) {
            java.time.LocalDate endOfFirstPhase = appStartDate.plusDays(11); // 12 zile inclusiv startul
            if (!current.isBefore(appStartDate) && !current.isAfter(endOfFirstPhase)) {
                return true;
            }
        }

        // Verificăm perioada curentă declanșată manual
        if (currentTestingPhaseStart != null) {
            java.time.LocalDate endOfPhase = currentTestingPhaseStart.plusDays(11);
            if (!current.isBefore(currentTestingPhaseStart) && !current.isAfter(endOfPhase)) {
                return true;
            }
        }

        return false;
    }

    private void checkMilestoneDeadlines(LocalDate dateToCheck) {
        for (Milestone m : milestones) {
            // Ignorăm milestone-urile terminate sau blocate
            if (isMilestoneFinished(m) || isMilestoneBlocked(m)) continue;

            LocalDate due = LocalDate.parse(m.getDueDate());

            // Regula: O zi calendaristică înainte (due minus 1 zi)
            if (dateToCheck.equals(due.minusDays(1))) {
                String msg = "Milestone " + m.getName() + " is due tomorrow. All unresolved tickets are now CRITICAL.";
                // Aceasta va adăuga mesajul în listele Userilor (fără output la consolă)
                notifyAssignedDevelopers(m, msg);
            }
        }
    }



    // În Database.java

    public void checkDependenciesAfterClosingTicket(Ticket closedTicket) {
        // 1. Găsim milestone-ul din care face parte tichetul
        Milestone parentMilestone = findMilestoneForTicket(closedTicket.getId());
        if (parentMilestone == null || parentMilestone.getBlockingFor() == null) return;

        // 2. Verificăm dacă milestone-ul curent este ACUM terminat (toate tichetele closed)
        if (!isMilestoneFinished(parentMilestone)) return;

        // 3. Dacă e terminat, vedem ce milestone-uri deblochează
        for (String blockedMilestoneName : parentMilestone.getBlockingFor()) {
            Milestone blockedM = findMilestoneByName(blockedMilestoneName); // Trebuie implementată

            // Verificăm dacă blockedM este acum complet deblocat (nu mai are alte dependențe active)
            if (blockedM != null && !isMilestoneBlocked(blockedM)) {
                LocalDate due = LocalDate.parse(blockedM.getDueDate());

                // Regula: Dacă s-a deblocat DUPĂ deadline
                if (currentSystemDate.isAfter(due)) {
                    String msg = "Milestone " + blockedM.getName() + " was unblocked after due date. All active tickets are now CRITICAL.";
                    notifyAssignedDevelopers(blockedM, msg);

                    // TODO: Aici ar trebui setate și tichetele pe CRITICAL, conform enunțului
                }
            }
        }
    }

    public Milestone findMilestoneByName(String name) {
        return milestones.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
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
    public void reset() {
        users.clear();
        tickets.clear();
        milestones.clear();
        ticketIdCounter = 0;
        appStartDate = null;
        appClosed = false;

        // FIX: Resetează și data curentă pentru a nu afecta testele următoare
        currentSystemDate = null;
    }
}