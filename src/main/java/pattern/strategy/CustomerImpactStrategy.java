package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.featureRequest;
import model.enums.ticketStatus;
import repository.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomerImpactStrategy implements MetricStrategy {

    @Override
    public ObjectNode calculate(ObjectMapper mapper, Database db) {
        ObjectNode reportNode = mapper.createObjectNode();

        // 1. Filtrare tichete: Doar OPEN și IN_PROGRESS
        List<Ticket> relevantTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == ticketStatus.OPEN || t.getStatus() == ticketStatus.IN_PROGRESS)
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
                    score = calculateFeatureScore((featureRequest) t);
                    break;
                case UI_FEEDBACK:
                    score = calculateUIScore((UIFeedback) t);
                    break;
            }
            totalScore.put(t.getType().toString(), totalScore.get(t.getType().toString()) + score);
        }

        ObjectNode impactNode = reportNode.putObject("customerImpactByType");

        putAverage(impactNode, "BUG", totalScore.get("BUG"), byType.get("BUG"));
        putAverage(impactNode, "FEATURE_REQUEST", totalScore.get("FEATURE_REQUEST"), byType.get("FEATURE_REQUEST"));
        putAverage(impactNode, "UI_FEEDBACK", totalScore.get("UI_FEEDBACK"), byType.get("UI_FEEDBACK"));

        return reportNode;
    }

    private void putAverage(ObjectNode node, String key, double total, int count) {
        if (count == 0) {
            node.put(key, 0.0);
        } else {
            double avg = total / count;
            node.put(key, Math.round(avg * 100.0) / 100.0);
        }
    }

    // --- FORMULE ACTUALIZATE ---

    private double calculateBugScore(Bug b) {
        // Formula: frequency * businessPriority * severityFactor
        // Max: 4 * 4 * 3 = 48
        int f = getFrequencyValue(b.getFrequency());
        int p = getPriorityValue(b.getBusinessPriority());
        int s = getSeverityValue(b.getSeverity());

        double raw = (double) (f * p * s);
        return (raw * 100.0) / 48.0;
    }

    private double calculateFeatureScore(featureRequest f) {
        // Formula: businessValue * customerDemand
        // Max: 10 * 10 = 100
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());

        double raw = (double) (v * d);
        return (raw * 100.0) / 100.0;
    }

    private double calculateUIScore(UIFeedback u) {
        // Formula: businessValue * usabilityScore
        // Max: 10 * 10 = 100 (usability e max 10, value e max 10)
        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;

        double raw = (double) (v * usb);
        return (raw * 100.0) / 100.0;
    }

    // --- HELPER VALUES ---

    private int getPriorityValue(model.enums.ticketPriority p) {
        switch (p) {
            case LOW: return 1;
            case MEDIUM: return 2;
            case HIGH: return 3;
            case CRITICAL: return 4;
            default: return 0;
        }
    }

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

    // ATENTIE: Valori 1, 3, 6, 10
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

    // ATENTIE: Valori 1, 3, 6, 10
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