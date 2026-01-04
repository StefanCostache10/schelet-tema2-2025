package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.featureRequest;
import model.enums.ticketStatus;
import repository.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TicketRiskStrategy implements MetricStrategy {

    @Override
    public ObjectNode calculate(ObjectMapper mapper, Database db) {
        ObjectNode reportNode = mapper.createObjectNode();

        List<Ticket> activeTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == ticketStatus.OPEN || t.getStatus() == ticketStatus.IN_PROGRESS)
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

            String prio = t.getBusinessPriority().toString();
            byPriority.put(prio, byPriority.getOrDefault(prio, 0) + 1);

            double score = 0.0;
            switch (t.getType()) {
                case BUG:
                    score = calculateBugRisk((Bug) t);
                    break;
                case FEATURE_REQUEST:
                    score = calculateFeatureRisk((featureRequest) t);
                    break;
                case UI_FEEDBACK:
                    score = calculateUIRisk((UIFeedback) t);
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
        riskNode.put("FEATURE_REQUEST", getRiskLabel(calculateAverage(scoresByType.get("FEATURE_REQUEST"))));
        riskNode.put("UI_FEEDBACK", getRiskLabel(calculateAverage(scoresByType.get("UI_FEEDBACK"))));

        return reportNode;
    }

    private double calculateAverage(List<Double> scores) {
        if (scores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Double s : scores) sum += s;
        return sum / scores.size();
    }

    private String getRiskLabel(double score) {
        if (score < 25.0) return "NEGLIGIBLE";
        if (score < 50.0) return "MODERATE";
        if (score < 75.0) return "SIGNIFICANT";
        return "MAJOR";
    }

    // --- FORMULE RISC ---

    private double calculateBugRisk(Bug b) {
        // Formula: frequency * severityFactor
        // Max: 4 * 3 = 12
        int f = getFrequencyValue(b.getFrequency());
        int s = getSeverityValue(b.getSeverity());
        double raw = (double) (f * s);
        return (raw * 100.0) / 12.0;
    }

    private double calculateFeatureRisk(featureRequest f) {
        // Formula: businessValue + customerDemand
        // Max: 10 + 10 = 20
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());
        double raw = (double) (v + d);
        return (raw * 100.0) / 20.0;
    }

    private double calculateUIRisk(UIFeedback u) {
        // Formula: (11 - usabilityScore) * businessValue
        // Max: (11 - 1) * 10 = 100
        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;
        double raw = (double) ((11 - usb) * v);
        return (raw * 100.0) / 100.0;
    }

    // --- HELPER VALUES (Same as CustomerImpact) ---
    // Repetă metodele private pentru consistență

    private int getSeverityValue(String s) {
        if (s == null) return 0;
        switch (s) {
            case "MINOR": return 1;
            case "MODERATE": return 2;
            case "SEVERE": return 3;
            default: return 0;
        }
    }

    private int getFrequencyValue(String f) {
        if (f == null) return 0;
        switch (f) {
            case "RARE": return 1;
            case "OCCASIONAL": return 2;
            case "FREQUENT": return 3;
            case "ALWAYS": return 4;
            default: return 0;
        }
    }

    private int getBusinessValue(String val) {
        if (val == null) return 0;
        switch (val) {
            case "S": return 1;
            case "M": return 3;
            case "L": return 6;
            case "XL": return 10;
            default: return 0;
        }
    }

    private int getDemandValue(String d) {
        if (d == null) return 0;
        switch (d) {
            case "LOW": return 1;
            case "MEDIUM": return 3;
            case "HIGH": return 6;
            case "VERY_HIGH": return 10;
            default: return 0;
        }
    }
}