package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.TicketPriority;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.FeatureRequest;
import model.enums.TicketStatus;
import repository.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CustomerImpactStrategy implements MetricStrategy {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double BUG_MAX_SCORE = 48.0;

    private static final int PRIORITY_LOW = 1;
    private static final int PRIORITY_MEDIUM = 2;
    private static final int PRIORITY_HIGH = 3;
    private static final int PRIORITY_CRITICAL = 4;

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

        // 1. Filtrare tichete: Doar OPEN și IN_PROGRESS
        List<Ticket> relevantTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == TicketStatus.OPEN
                        || t.getStatus() == TicketStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        reportNode.put("totalTickets", relevantTickets.size());

        // 2. Numărare pe Tipuri
        Map<String, Integer> byType = new HashMap<>();
        byType.put("BUG", 0);
        byType.put("FEATURE_REQUEST", 0);
        byType.put("UI_FEEDBACK", 0);

        for (Ticket t : relevantTickets) {
            String typeStr = t.getType().toString();
            byType.put(typeStr, byType.getOrDefault(typeStr, 0) + 1);
        }

        ObjectNode typeNode = reportNode.putObject("ticketsByType");
        byType.forEach(typeNode::put);

        // 3. Numărare pe Prioritate
        Map<String, Integer> byPriority = new HashMap<>();
        byPriority.put("LOW", 0);
        byPriority.put("MEDIUM", 0);
        byPriority.put("HIGH", 0);
        byPriority.put("CRITICAL", 0);

        for (Ticket t : relevantTickets) {
            String p = t.getBusinessPriority().toString();
            byPriority.put(p, byPriority.getOrDefault(p, 0) + 1);
        }

        ObjectNode priorityNode = reportNode.putObject("ticketsByPriority");
        priorityNode.put("LOW", byPriority.get("LOW"));
        priorityNode.put("MEDIUM", byPriority.get("MEDIUM"));
        priorityNode.put("HIGH", byPriority.get("HIGH"));
        priorityNode.put("CRITICAL", byPriority.get("CRITICAL"));

        // 4. Calculare Scoruri și Medii
        Map<String, Double> totalScore = new HashMap<>();
        totalScore.put("BUG", 0.0);
        totalScore.put("FEATURE_REQUEST", 0.0);
        totalScore.put("UI_FEEDBACK", 0.0);

        for (Ticket t : relevantTickets) {
            double score = 0.0;
            switch (t.getType()) {
                case BUG:
                    score = calculateBugScore((Bug) t);
                    break;
                case FEATURE_REQUEST:
                    score = calculateFeatureScore((FeatureRequest) t);
                    break;
                case UI_FEEDBACK:
                    score = calculateUIScore((UIFeedback) t);
                    break;
                default:
                    break;
            }
            totalScore.put(t.getType().toString(),
                    totalScore.get(t.getType().toString()) + score);
        }

        ObjectNode impactNode = reportNode.putObject("customerImpactByType");

        putAverage(impactNode, "BUG", totalScore.get("BUG"), byType.get("BUG"));
        putAverage(impactNode, "FEATURE_REQUEST",
                totalScore.get("FEATURE_REQUEST"), byType.get("FEATURE_REQUEST"));
        putAverage(impactNode, "UI_FEEDBACK",
                totalScore.get("UI_FEEDBACK"), byType.get("UI_FEEDBACK"));

        return reportNode;
    }

    private void putAverage(final ObjectNode node, final String key,
                            final double total, final int count) {
        if (count == 0) {
            node.put(key, 0.0);
        } else {
            double avg = total / count;
            node.put(key, Math.round(avg * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER);
        }
    }


    private double calculateBugScore(final Bug b) {
        int f = getFrequencyValue(b.getFrequency());
        int p = getPriorityValue(b.getBusinessPriority());
        int s = getSeverityValue(b.getSeverity());

        double raw = (double) (f * p * s);
        return (raw * PERCENTAGE_MULTIPLIER) / BUG_MAX_SCORE;
    }

    private double calculateFeatureScore(final FeatureRequest f) {
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());

        double raw = (double) (v * d);
        return (raw * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
    }

    private double calculateUIScore(final UIFeedback u) {

        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;

        double raw = (double) (v * usb);
        return (raw * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
    }


    private int getPriorityValue(final TicketPriority p) {
        switch (p) {
            case LOW: return PRIORITY_LOW;
            case MEDIUM: return PRIORITY_MEDIUM;
            case HIGH: return PRIORITY_HIGH;
            case CRITICAL: return PRIORITY_CRITICAL;
            default: return 0;
        }
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
