package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.Role;
import model.enums.Seniority;
import model.enums.TicketPriority;
import model.enums.TicketStatus;
import model.enums.TicketType;
import model.ticket.Ticket;
import model.user.Developer;
import model.user.Manager;
import model.user.User;
import repository.Database;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class PerformanceStrategy implements MetricStrategy {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double JUNIOR_BASE_SCORE = 5.0;
    private static final double MID_BASE_SCORE = 15.0;
    private static final double SENIOR_BASE_SCORE = 30.0;

    private static final double JUNIOR_TICKET_FACTOR = 0.5;
    private static final double MID_TICKET_FACTOR = 0.5;
    private static final double MID_PRIO_FACTOR = 0.7;
    private static final double MID_TIME_FACTOR = 0.3;
    private static final double SENIOR_TICKET_FACTOR = 0.5;
    private static final double SENIOR_PRIO_FACTOR = 1.0;
    private static final double SENIOR_TIME_FACTOR = 0.5;

    private static final double TYPES_COUNT = 3.0;

    private final String commandTimestamp;
    private final String managerUsername;

    public PerformanceStrategy(final String managerUsername, final String commandTimestamp) {
        this.managerUsername = managerUsername;
        this.commandTimestamp = commandTimestamp;
    }

    @Override
    public ObjectNode calculate(final ObjectMapper mapper, final Database db) {
        ObjectNode wrapper = mapper.createObjectNode();
        ArrayNode devsArray = wrapper.putArray("result");

        User managerUser = db.findUserByUsername(managerUsername);
        if (managerUser == null || managerUser.getRole() != Role.MANAGER) {
            return wrapper;
        }
        Manager manager = (Manager) managerUser;
        List<String> subordinates = manager.getSubordinates();

        LocalDate cmdDate = LocalDate.parse(commandTimestamp);
        YearMonth targetMonth = YearMonth.from(cmdDate).minusMonths(1);

        List<Developer> developers = subordinates.stream()
                .map(db::findUserByUsername)
                .filter(u -> u != null && u.getRole() == Role.DEVELOPER)
                .map(u -> (Developer) u)
                .sorted(Comparator.comparing(User::getUsername))
                .collect(Collectors.toList());

        for (Developer dev : developers) {
            ObjectNode devNode = mapper.createObjectNode();
            devNode.put("username", dev.getUsername());

            List<Ticket> devTickets = db.getTickets().stream()
                    .filter(t -> t.getStatus() == TicketStatus.CLOSED)
                    .filter(t -> dev.getUsername().equals(t.getAssignedTo()))
                    .filter(t -> isClosedInMonth(t, targetMonth))
                    .collect(Collectors.toList());

            int closedCount = devTickets.size();
            devNode.put("closedTickets", closedCount);

            double avgResTime = calculateAverageResolutionTime(devTickets);
            devNode.put("averageResolutionTime", round(avgResTime));

            double score = calculateScore(dev, devTickets, avgResTime, db);
            dev.setPerformanceScore(score);
            devNode.put("performanceScore", round(score));
            devNode.put("seniority", dev.getSeniority().toString());


            devsArray.add(devNode);
        }

        return wrapper;
    }

    private boolean isClosedInMonth(final Ticket t, final YearMonth targetMonth) {
        if (t.getClosedAt() == null || t.getClosedAt().isEmpty()) {
            return false;
        }
        LocalDate closedDate = LocalDate.parse(t.getClosedAt());
        return YearMonth.from(closedDate).equals(targetMonth);
    }

    private double calculateAverageResolutionTime(final List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return 0.0;
        }
        double totalDays = 0;
        for (Ticket t : tickets) {
            if (t.getAssignedAt() != null && !t.getAssignedAt().isEmpty()
                    && t.getSolvedAt() != null && !t.getSolvedAt().isEmpty()) {

                LocalDate start = LocalDate.parse(t.getAssignedAt());
                LocalDate end = LocalDate.parse(t.getSolvedAt());
                long days = ChronoUnit.DAYS.between(start, end);
                totalDays += (days + 1);
            }
        }
        return totalDays / tickets.size();
    }

    private double calculateScore(final Developer dev, final List<Ticket> tickets,
                                  final double avgResTime, final Database db) {
        if (tickets.isEmpty()) {
            return 0.0;
        }

        int closedTickets = tickets.size();
        Seniority seniority = dev.getSeniority();
        double score = 0.0;

        switch (seniority) {
            case JUNIOR:
                score = calculateJuniorScore(tickets, closedTickets);
                break;
            case MID:
                score = calculateMidScore(tickets, closedTickets, avgResTime, db);
                break;
            case SENIOR:
                score = calculateSeniorScore(tickets, closedTickets, avgResTime, db);
                break;
            default:
                break;
        }

        return Math.max(0.0, score);
    }

    private double calculateJuniorScore(final List<Ticket> tickets, final int closedTickets) {
        long bugCount = tickets.stream().filter(t -> t.getType() == TicketType.BUG).count();
        long featureCount = tickets.stream()
                .filter(t -> t.getType() == TicketType.FEATURE_REQUEST).count();
        long uiCount = tickets.stream().filter(t -> t.getType() == TicketType.UI_FEEDBACK).count();

        double avgType = (bugCount + featureCount + uiCount) / TYPES_COUNT;
        double variance = (Math.pow(bugCount - avgType, 2)
                + Math.pow(featureCount - avgType, 2)
                + Math.pow(uiCount - avgType, 2)) / TYPES_COUNT;
        double stdDev = Math.sqrt(variance);

        double diversityFactor = (avgType == 0) ? 0.0 : stdDev / avgType;

        double base = JUNIOR_TICKET_FACTOR * closedTickets - diversityFactor;
        return Math.max(0, base) + JUNIOR_BASE_SCORE;
    }

    private double calculateMidScore(final List<Ticket> tickets, final int closedTickets,
                                     final double avgResTime, final Database db) {
        long highPrio = tickets.stream()
                .filter(t -> {
                    TicketPriority p = db.getCalculatedPriority(t, t.getClosedAt());
                    return p == TicketPriority.HIGH || p == TicketPriority.CRITICAL;
                })
                .count();

        double base = MID_TICKET_FACTOR * closedTickets
                + MID_PRIO_FACTOR * highPrio
                - MID_TIME_FACTOR * avgResTime;
        return Math.max(0, base) + MID_BASE_SCORE;
    }

    private double calculateSeniorScore(final List<Ticket> tickets, final int closedTickets,
                                        final double avgResTime, final Database db) {
        long highPrio = tickets.stream()
                .filter(t -> {
                    TicketPriority p = db.getCalculatedPriority(t, t.getClosedAt());
                    return p == TicketPriority.HIGH || p == TicketPriority.CRITICAL;
                })
                .count();

        double base = SENIOR_TICKET_FACTOR * closedTickets
                + SENIOR_PRIO_FACTOR * highPrio
                - SENIOR_TIME_FACTOR * avgResTime;
        return Math.max(0, base) + SENIOR_BASE_SCORE;
    }

    private double round(final double value) {
        return Math.round(value * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
    }
}
