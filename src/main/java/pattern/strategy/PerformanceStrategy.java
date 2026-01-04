package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.user.Developer;
import model.user.Manager;
import model.user.User;
import model.enums.Role;
import model.enums.Seniority;
import model.enums.ticketPriority;
import model.enums.ticketStatus;
import repository.Database;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PerformanceStrategy implements MetricStrategy {

    private final String commandTimestamp;
    private final String managerUsername;

    public PerformanceStrategy(String managerUsername, String commandTimestamp) {
        this.managerUsername = managerUsername;
        this.commandTimestamp = commandTimestamp;
    }

    @Override
    public ObjectNode calculate(ObjectMapper mapper, Database db) {
        // Creăm un wrapper temporar pentru a respecta semnătura (return ObjectNode)
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
                .filter(u -> u instanceof Developer)
                .map(u -> (Developer) u)
                .sorted(Comparator.comparing(User::getUsername))
                .collect(Collectors.toList());

        for (Developer dev : developers) {
            ObjectNode devNode = mapper.createObjectNode();
            devNode.put("username", dev.getUsername());

            List<Ticket> devTickets = db.getTickets().stream()
                    .filter(t -> t.getStatus() == ticketStatus.CLOSED)
                    .filter(t -> dev.getUsername().equals(t.getAssignedTo()))
                    .filter(t -> isClosedInMonth(t, targetMonth))
                    .collect(Collectors.toList());

            int closedCount = devTickets.size();
            devNode.put("closedTickets", closedCount);

            double avgResTime = calculateAverageResolutionTime(devTickets);
            devNode.put("averageResolutionTime", round(avgResTime));

            double score = calculateScore(dev, devTickets, avgResTime, db);
            devNode.put("performanceScore", round(score));
            devNode.put("seniority", dev.getSeniority().toString());

            devsArray.add(devNode);
        }

        return wrapper;
    }

    private boolean isClosedInMonth(Ticket t, YearMonth targetMonth) {
        if (t.getClosedAt() == null || t.getClosedAt().isEmpty()) return false;
        LocalDate closedDate = LocalDate.parse(t.getClosedAt());
        return YearMonth.from(closedDate).equals(targetMonth);
    }

    private double calculateAverageResolutionTime(List<Ticket> tickets) {
        if (tickets.isEmpty()) return 0.0;
        double totalDays = 0;
        for (Ticket t : tickets) {
            if (t.getAssignedAt() != null && !t.getAssignedAt().isEmpty() &&
                    t.getSolvedAt() != null && !t.getSolvedAt().isEmpty()) {

                LocalDate start = LocalDate.parse(t.getAssignedAt());
                LocalDate end = LocalDate.parse(t.getSolvedAt());
                long days = ChronoUnit.DAYS.between(start, end);
                totalDays += (days + 1);
            }
        }
        return totalDays / tickets.size();
    }

    private double calculateScore(Developer dev, List<Ticket> tickets, double avgResTime, Database db) {
        if (tickets.isEmpty()) return 0.0;

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
        }

        return Math.max(0.0, score);
    }

    private double calculateJuniorScore(List<Ticket> tickets, int closedTickets) {
        long bugCount = tickets.stream().filter(t -> t.getType() == model.enums.ticketType.BUG).count();
        long featureCount = tickets.stream().filter(t -> t.getType() == model.enums.ticketType.FEATURE_REQUEST).count();
        long uiCount = tickets.stream().filter(t -> t.getType() == model.enums.ticketType.UI_FEEDBACK).count();

        double avgType = (bugCount + featureCount + uiCount) / 3.0;
        double variance = (Math.pow(bugCount - avgType, 2) + Math.pow(featureCount - avgType, 2) + Math.pow(uiCount - avgType, 2)) / 3.0;
        double stdDev = Math.sqrt(variance);

        double diversityFactor = (avgType == 0) ? 0.0 : stdDev / avgType;

        double base = 0.5 * closedTickets - diversityFactor;
        return Math.max(0, base) + 5.0;
    }

    private double calculateMidScore(List<Ticket> tickets, int closedTickets, double avgResTime, Database db) {
        long highPrio = tickets.stream()
                .filter(t -> {
                    ticketPriority p = db.getCalculatedPriority(t, t.getSolvedAt());
                    return p == ticketPriority.HIGH || p == ticketPriority.CRITICAL;
                })
                .count();

        double base = 0.5 * closedTickets + 0.7 * highPrio - 0.3 * avgResTime;
        return Math.max(0, base) + 15.0;
    }

    private double calculateSeniorScore(List<Ticket> tickets, int closedTickets, double avgResTime, Database db) {
        long highPrio = tickets.stream()
                .filter(t -> {
                    ticketPriority p = db.getCalculatedPriority(t, t.getSolvedAt());
                    return p == ticketPriority.HIGH || p == ticketPriority.CRITICAL;
                })
                .count();

        double base = 0.5 * closedTickets + 1.0 * highPrio - 0.5 * avgResTime;
        return Math.max(0, base) + 30.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}