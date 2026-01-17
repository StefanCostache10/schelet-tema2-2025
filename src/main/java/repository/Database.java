package repository;

import model.Milestone;
import model.enums.TicketPriority;
import model.enums.TicketStatus;
import model.ticket.Ticket;
import model.user.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class Database {
    private static final int DAYS_FOR_PRIORITY_INCREASE = 3;
    private static final int TESTING_PHASE_DURATION_DAYS = 11;

    private static Database instance;
    private List<User> users = new ArrayList<>();
    private List<Ticket> tickets = new ArrayList<>();
    private List<Milestone> milestones = new ArrayList<>();
    private int ticketIdCounter = 0;
    private LocalDate appStartDate;
    private boolean appClosed = false;
    private LocalDate currentSystemDate;
    private java.time.LocalDate currentTestingPhaseStart;

    private Database() {
    }

    /**
     * Returns the singleton instance of the Database.
     *
     * @return the singleton instance
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Calculates the priority of a ticket based on the current timestamp.
     *
     * @param ticket           the ticket to calculate priority for
     * @param currentTimestamp the current timestamp string
     * @return the calculated TicketPriority
     */
    public TicketPriority getCalculatedPriority(final Ticket ticket,
                                                final String currentTimestamp) {
        Milestone m = findMilestoneForTicket(ticket.getId());
        if (m == null) {
            return ticket.getBusinessPriority();
        }
        if (isMilestoneBlocked(m)) {
            return ticket.getBusinessPriority();
        }

        LocalDate now = LocalDate.parse(currentTimestamp);
        LocalDate created = LocalDate.parse(m.getCreatedAt());
        LocalDate due = LocalDate.parse(m.getDueDate());

        if (now.isAfter(due.minusDays(2))) {
            return TicketPriority.CRITICAL;
        }

        long days = ChronoUnit.DAYS.between(created, now);
        int steps = (int) (days / DAYS_FOR_PRIORITY_INCREASE);

        TicketPriority p = ticket.getBusinessPriority();
        for (int i = 0; i < steps; i++) {
            p = p.next();
        }

        checkPrioritySeniorityConflict(ticket, p);

        return p;
    }

    /**
     * Updates the current system date.
     *
     * @param timestampStr the new date string
     */
    public void updateCurrentDate(final String timestampStr) {
        LocalDate newDate = LocalDate.parse(timestampStr);

        if (currentSystemDate == null) {
            currentSystemDate = newDate;
            // Check inițial (poate inputul începe direct cu o zi înainte de deadline)
            checkMilestoneDeadlines(currentSystemDate);
            return;
        }


        while (currentSystemDate.isBefore(newDate)) {
            currentSystemDate = currentSystemDate.plusDays(1);
            checkMilestoneDeadlines(currentSystemDate);
        }
    }

    /**
     * Starts a new testing phase.
     *
     * @param timestamp the start date string
     */
    public void startNewTestingPhase(final String timestamp) {
        this.currentTestingPhaseStart = java.time.LocalDate.parse(timestamp);
    }

    /**
     * Checks if the system is currently in a testing phase.
     *
     * @param currentTimestampStr the current timestamp string
     * @return true if in testing phase, false otherwise
     */
    public boolean isInTestingPhase(final String currentTimestampStr) {
        java.time.LocalDate current = java.time.LocalDate.parse(currentTimestampStr);

        if (appStartDate != null) {
            java.time.LocalDate endOfFirstPhase = appStartDate.plusDays(TESTING_PHASE_DURATION_DAYS);
            if (!current.isBefore(appStartDate) && !current.isAfter(endOfFirstPhase)) {
                return true;
            }
        }

        if (currentTestingPhaseStart != null) {
            java.time.LocalDate endOfPhase = currentTestingPhaseStart
                    .plusDays(TESTING_PHASE_DURATION_DAYS);
            if (!current.isBefore(currentTestingPhaseStart) && !current.isAfter(endOfPhase)) {
                return true;
            }
        }

        return false;
    }

    private void checkMilestoneDeadlines(final LocalDate dateToCheck) {
        for (Milestone m : milestones) {
            if (isMilestoneFinished(m) || isMilestoneBlocked(m)) {
                continue;
            }

            LocalDate due = LocalDate.parse(m.getDueDate());

            if (dateToCheck.equals(due.minusDays(1))) {
                String msg = "Milestone " + m.getName()
                        + " is due tomorrow. All unresolved tickets are now CRITICAL.";
                notifyAssignedDevelopers(m, msg);
            }
        }
    }

    /**
     * Checks dependencies after closing a ticket.
     *
     * @param closedTicket the ticket that was closed
     */
    public void checkDependenciesAfterClosingTicket(final Ticket closedTicket) {
        Milestone parentMilestone = findMilestoneForTicket(closedTicket.getId());
        if (parentMilestone == null || parentMilestone.getBlockingFor() == null) {
            return;
        }

        if (!isMilestoneFinished(parentMilestone)) {
            return;
        }

        for (String blockedMilestoneName : parentMilestone.getBlockingFor()) {
            Milestone blockedM = findMilestoneByName(blockedMilestoneName);

            if (blockedM != null && !isMilestoneBlocked(blockedM)) {
                LocalDate due = LocalDate.parse(blockedM.getDueDate());

                if (currentSystemDate.isAfter(due)) {
                    String msg = "Milestone " + blockedM.getName()
                            + " was unblocked after due date. All active tickets are now CRITICAL.";
                    notifyAssignedDevelopers(blockedM, msg);

                }
            }
        }
    }

    /**
     * Finds a milestone by its name.
     *
     * @param name the name of the milestone
     * @return the milestone or null if not found
     */
    public Milestone findMilestoneByName(final String name) {
        return milestones.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void checkPrioritySeniorityConflict(final Ticket ticket,
                                                final TicketPriority calculated) {
        if (ticket.getStatus() == TicketStatus.IN_PROGRESS && !ticket.getAssignedTo().isEmpty()) {
            User dev = findUserByUsername(ticket.getAssignedTo());
            if (dev instanceof model.user.Developer) {
                return;
            }
        }
    }

    /**
     * Checks if a milestone is blocked by other milestones.
     *
     * @param m the milestone to check
     * @return true if blocked, false otherwise
     */
    public boolean isMilestoneBlocked(final Milestone m) {
        return milestones.stream().anyMatch(other ->
                other.getBlockingFor() != null
                        && other.getBlockingFor().contains(m.getName())
                        && !isMilestoneFinished(other));
    }

    private boolean isMilestoneFinished(final Milestone m) {
        return m.getTickets().stream()
                .map(this::findTicketById)
                .allMatch(t -> t != null && t.getStatus() == TicketStatus.CLOSED);
    }

    /**
     * Notifies a user with a message.
     *
     * @param username the username of the user
     * @param message  the message to send
     */
    public void notifyUser(final String username, final String message) {
        User user = findUserByUsername(username);
        if (user != null) {
            user.update(message);
        }
    }

    /**
     * Notifies all assigned developers of a milestone.
     *
     * @param milestone the milestone
     * @param message   the message to send
     */
    public void notifyAssignedDevelopers(final Milestone milestone, final String message) {
        if (milestone.getAssignedDevs() != null) {
            for (String devUsername : milestone.getAssignedDevs()) {
                notifyUser(devUsername, message);
            }
        }
    }

    public void closeApp() {
        this.appClosed = true;
    }

    public boolean isAppClosed() {
        return appClosed;
    }

    public void setUsers(final List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public void addTicket(final Ticket t) {
        t.setId(ticketIdCounter++);
        tickets.add(t);
    }

    public void addMilestone(final Milestone m) {
        milestones.add(m);
    }

    public Ticket findTicketById(final int id) {
        return tickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    public User findUserByUsername(final String u) {
        return users.stream().filter(user -> user.getUsername().equals(u))
                .findFirst().orElse(null);
    }

    public Milestone findMilestoneForTicket(final int id) {
        return milestones.stream().filter(m -> m.getTickets().contains(id))
                .findFirst().orElse(null);
    }

    public LocalDate getAppStartDate() {
        return appStartDate;
    }

    public void setAppStartDate(final LocalDate d) {
        this.appStartDate = d;
    }

    public void reset() {
        users.clear();
        tickets.clear();
        milestones.clear();
        ticketIdCounter = 0;
        appStartDate = null;
        appClosed = false;

        currentSystemDate = null;
    }
}
