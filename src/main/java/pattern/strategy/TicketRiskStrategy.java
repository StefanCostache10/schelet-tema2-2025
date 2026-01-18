package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.FeatureRequest;
import model.enums.TicketStatus;
import repository.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TicketRiskStrategy implements MetricStrategy {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double BUG_MAX_SCORE = 12.0;
    private static final double FEATURE_MAX_SCORE = 20.0;
    private static final double UI_MAX_SCORE = 100.0;
    private static final int UI_USABILITY_BASE = 11;

    private static final double RISK_NEGLIGIBLE_THRESHOLD = 25.0;
    private static final double RISK_MODERATE_THRESHOLD = 50.0;
    private static final double RISK_SIGNIFICANT_THRESHOLD = 75.0;

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

        List<Ticket> activeTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == TicketStatus.OPEN
                        || t.getStatus() == TicketStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        reportNode.put("totalTickets", activeTickets.size());

        Map<String, Integer> byType = new HashMap<>();
        byType.put("BUG", 0);
        byType.put("FEATURE_REQUEST", 0);
        byType.put("UI_FEEDBACK", 0);

        Map<String, Integer> byPriority = new HashMap<>();
        byPriority.put("LOW", 0);
        byPriority.put("MEDIUM", 0);
        byPriority.put("HIGH", 0);
        byPriority.put("CRITICAL", 0);

        Map<String, List<Double>> scoresByType = new HashMap<>();
        scoresByType.put("BUG", new ArrayList<>());
        scoresByType.put("FEATURE_REQUEST", new ArrayList<>());
        scoresByType.put("UI_FEEDBACK", new ArrayList<>());

        for (Ticket t : activeTickets) {
            String type = t.getType().toString();
            byType.put(type, byType.getOrDefault(type, 0) + 1);

            // Use calculated priority based on current date
            String currentDate = db.getCurrentSystemDate().toString();
            String prio = db.getCalculatedPriority(t, currentDate).toString();
            byPriority.put(prio, byPriority.getOrDefault(prio, 0) + 1);

            double score = 0.0;
            switch (t.getType()) {
                case BUG:
                    score = calculateBugRisk((Bug) t);
                    break;
                case FEATURE_REQUEST:
                    score = calculateFeatureRisk((FeatureRequest) t);
                    break;
                case UI_FEEDBACK:
                    score = calculateUIRisk((UIFeedback) t);
                    break;
                default:
                    break;
            }
            scoresByType.get(type).add(score);
        }

        ObjectNode ticketsByTypeNode = reportNode.putObject("ticketsByType");
        byType.forEach(ticketsByTypeNode::put);

        ObjectNode ticketsByPriorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriorityNode.put("LOW", byPriority.get("LOW"));
        ticketsByPriorityNode.put("MEDIUM", byPriority.get("MEDIUM"));
        ticketsByPriorityNode.put("HIGH", byPriority.get("HIGH"));
        ticketsByPriorityNode.put("CRITICAL", byPriority.get("CRITICAL"));

        ObjectNode riskNode = reportNode.putObject("riskByType");
        riskNode.put("BUG", getRiskLabel(calculateAverage(scoresByType.get("BUG"))));
        riskNode.put("FEATURE_REQUEST",
                getRiskLabel(calculateAverage(scoresByType.get("FEATURE_REQUEST"))));
        riskNode.put("UI_FEEDBACK",
                getRiskLabel(calculateAverage(scoresByType.get("UI_FEEDBACK"))));

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
        return sum / scores.size();
    }

    private String getRiskLabel(final double score) {
        if (score < RISK_NEGLIGIBLE_THRESHOLD) {
            return "NEGLIGIBLE";
        }
        if (score < RISK_MODERATE_THRESHOLD) {
            return "MODERATE";
        }
        if (score < RISK_SIGNIFICANT_THRESHOLD) {
            return "SIGNIFICANT";
        }
        return "MAJOR";
    }

    private double calculateBugRisk(final Bug b) {
        int f = getFrequencyValue(b.getFrequency());
        int s = getSeverityValue(b.getSeverity());
        double raw = (double) (f * s);
        return (raw * PERCENTAGE_MULTIPLIER) / BUG_MAX_SCORE;
    }

    private double calculateFeatureRisk(final FeatureRequest f) {
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());
        double raw = (double) (v + d);
        return (raw * PERCENTAGE_MULTIPLIER) / FEATURE_MAX_SCORE;
    }

    private double calculateUIRisk(final UIFeedback u) {
        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;
        double raw = (double) ((UI_USABILITY_BASE - usb) * v);
        return (raw * PERCENTAGE_MULTIPLIER) / UI_MAX_SCORE;
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
