package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.FeatureRequest;
import model.enums.TicketStatus;
import repository.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ResolutionEfficiencyStrategy implements MetricStrategy {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double BUG_MAX_SCORE = 70.0;
    private static final double FEATURE_MAX_SCORE = 20.0;
    private static final double UI_MAX_SCORE = 20.0;
    private static final double BUG_MULTIPLIER = 10.0;

    private static final int SEVERITY_MINOR = 1;
    private static final int SEVERITY_MODERATE = 2;
    private static final int SEVERITY_SEVERE = 3;

    private static final int FREQUENCY_RARE = 1;
    private static final int FREQUENCY_OCCASIONAL = 2;
    private static final int FREQUENCY_FREQUENT = 3;
    private static final int FREQUENCY_ALWAYS = 4;

    private static final int BUSINESS_VALUE_S = 1;
    private static final int BUSINESS_VALUE_M = 3;
    private static final int BUSINESS_VALUE_L = 6;
    private static final int BUSINESS_VALUE_XL = 10;

    private static final int DEMAND_LOW = 1;
    private static final int DEMAND_MEDIUM = 3;
    private static final int DEMAND_HIGH = 6;
    private static final int DEMAND_VERY_HIGH = 10;

    @Override
    public ObjectNode calculate(final ObjectMapper mapper, final Database db) {
        ObjectNode reportNode = mapper.createObjectNode();

        List<Ticket> closedTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == TicketStatus.RESOLVED
                        || t.getStatus() == TicketStatus.CLOSED)
                .collect(Collectors.toList());

        reportNode.put("totalTickets", closedTickets.size());

        Map<String, Integer> byType = new HashMap<>();
        byType.put("BUG", 0);
        byType.put("FEATURE_REQUEST", 0);
        byType.put("UI_FEEDBACK", 0);

        Map<String, Integer> byPriority = new HashMap<>();
        byPriority.put("LOW", 0);
        byPriority.put("MEDIUM", 0);
        byPriority.put("HIGH", 0);
        byPriority.put("CRITICAL", 0);

        Map<String, List<Double>> efficiencyScores = new HashMap<>();
        efficiencyScores.put("BUG", new ArrayList<>());
        efficiencyScores.put("FEATURE_REQUEST", new ArrayList<>());
        efficiencyScores.put("UI_FEEDBACK", new ArrayList<>());

        for (Ticket t : closedTickets) {
            String type = t.getType().toString();
            byType.put(type, byType.getOrDefault(type, 0) + 1);

            String prio = t.getBusinessPriority().toString();
            byPriority.put(prio, byPriority.getOrDefault(prio, 0) + 1);

            double days = calculateDays(t.getAssignedAt(), t.getSolvedAt());
            double score = 0.0;
            double maxScore = 1.0;

            switch (t.getType()) {
                case BUG:
                    score = calculateBugScore((Bug) t, days);
                    maxScore = BUG_MAX_SCORE;
                    break;
                case FEATURE_REQUEST:
                    score = calculateFeatureScore((FeatureRequest) t, days);
                    maxScore = FEATURE_MAX_SCORE;
                    break;
                case UI_FEEDBACK:
                    score = calculateUIScore((UIFeedback) t, days);
                    maxScore = UI_MAX_SCORE;
                    break;
                default:
                    break;
            }

            double finalEfficiency = (score * PERCENTAGE_MULTIPLIER) / maxScore;
            efficiencyScores.get(type).add(finalEfficiency);
        }

        ObjectNode ticketsByTypeNode = reportNode.putObject("ticketsByType");
        byType.forEach(ticketsByTypeNode::put);

        ObjectNode ticketsByPriorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriorityNode.put("LOW", byPriority.get("LOW"));
        ticketsByPriorityNode.put("MEDIUM", byPriority.get("MEDIUM"));
        ticketsByPriorityNode.put("HIGH", byPriority.get("HIGH"));
        ticketsByPriorityNode.put("CRITICAL", byPriority.get("CRITICAL"));

        ObjectNode efficiencyNode = reportNode.putObject("efficiencyByType");
        efficiencyNode.put("BUG", calculateAverage(efficiencyScores.get("BUG")));
        efficiencyNode.put("FEATURE_REQUEST",
                calculateAverage(efficiencyScores.get("FEATURE_REQUEST")));
        efficiencyNode.put("UI_FEEDBACK",
                calculateAverage(efficiencyScores.get("UI_FEEDBACK")));

        return reportNode;
    }

    private double calculateAverage(final List<Double> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double s : scores) {
            sum += s;
        }
        double avg = sum / scores.size();
        return Math.round(avg * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
    }

    private double calculateDays(final String assignedAt, final String solvedAt) {
        if (assignedAt == null || assignedAt.isEmpty() || solvedAt == null || solvedAt.isEmpty()) {
            return 1.0;
        }
        LocalDate start = LocalDate.parse(assignedAt);
        LocalDate end = LocalDate.parse(solvedAt);
        long days = ChronoUnit.DAYS.between(start, end) + 1; // Inclusiv ultima zi
        return Math.max(1.0, (double) days);
    }


    private double calculateBugScore(final Bug b, final double days) {
        int f = getFrequencyValue(b.getFrequency());
        int s = getSeverityValue(b.getSeverity());
        return (f + s) * BUG_MULTIPLIER / days;
    }

    private double calculateFeatureScore(final FeatureRequest f, final double days) {
        // (businessValue + customerDemand) / days
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());
        return (v + d) / days;
    }

    private double calculateUIScore(final UIFeedback u, final double days) {
        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;
        return (usb + v) / days;
    }


    private int getSeverityValue(final String s) {
        if (s == null) {
            return 0;
        }
        switch (s) {
            case "MINOR": return SEVERITY_MINOR;
            case "MODERATE": return SEVERITY_MODERATE;
            case "SEVERE": return SEVERITY_SEVERE;
            default: return 0;
        }
    }

    private int getFrequencyValue(final String f) {
        if (f == null) {
            return 0;
        }
        switch (f) {
            case "RARE": return FREQUENCY_RARE;
            case "OCCASIONAL": return FREQUENCY_OCCASIONAL;
            case "FREQUENT": return FREQUENCY_FREQUENT;
            case "ALWAYS": return FREQUENCY_ALWAYS;
            default: return 0;
        }
    }

    private int getBusinessValue(final String val) {
        if (val == null) {
            return 0;
        }
        switch (val) {
            case "S": return BUSINESS_VALUE_S;
            case "M": return BUSINESS_VALUE_M;
            case "L": return BUSINESS_VALUE_L;
            case "XL": return BUSINESS_VALUE_XL;
            default: return 0;
        }
    }

    private int getDemandValue(final String d) {
        if (d == null) {
            return 0;
        }
        switch (d) {
            case "LOW": return DEMAND_LOW;
            case "MEDIUM": return DEMAND_MEDIUM;
            case "HIGH": return DEMAND_HIGH;
            case "VERY_HIGH": return DEMAND_VERY_HIGH;
            default: return 0;
        }
    }
}
