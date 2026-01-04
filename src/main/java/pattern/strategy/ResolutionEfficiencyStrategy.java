package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Bug;
import model.ticket.Ticket;
import model.ticket.UIFeedback;
import model.ticket.featureRequest;
import model.enums.ticketStatus;
import repository.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResolutionEfficiencyStrategy implements MetricStrategy {

    @Override
    public ObjectNode calculate(ObjectMapper mapper, Database db) {
        ObjectNode reportNode = mapper.createObjectNode();

        // 1. Filtrare: Doar RESOLVED și CLOSED
        List<Ticket> closedTickets = db.getTickets().stream()
                .filter(t -> t.getStatus() == ticketStatus.RESOLVED || t.getStatus() == ticketStatus.CLOSED)
                .collect(Collectors.toList());

        reportNode.put("totalTickets", closedTickets.size());

        // 2. Statistici
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
                    maxScore = 70.0;
                    break;
                case FEATURE_REQUEST:
                    score = calculateFeatureScore((featureRequest) t, days);
                    maxScore = 20.0;
                    break;
                case UI_FEEDBACK:
                    score = calculateUIScore((UIFeedback) t, days);
                    maxScore = 20.0;
                    break;
            }

            double finalEfficiency = (score * 100.0) / maxScore;
            efficiencyScores.get(type).add(finalEfficiency);
        }

        // 3. Output
        ObjectNode ticketsByTypeNode = reportNode.putObject("ticketsByType");
        byType.forEach(ticketsByTypeNode::put);

        ObjectNode ticketsByPriorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriorityNode.put("LOW", byPriority.get("LOW"));
        ticketsByPriorityNode.put("MEDIUM", byPriority.get("MEDIUM"));
        ticketsByPriorityNode.put("HIGH", byPriority.get("HIGH"));
        ticketsByPriorityNode.put("CRITICAL", byPriority.get("CRITICAL"));

        ObjectNode efficiencyNode = reportNode.putObject("efficiencyByType");
        efficiencyNode.put("BUG", calculateAverage(efficiencyScores.get("BUG")));
        efficiencyNode.put("FEATURE_REQUEST", calculateAverage(efficiencyScores.get("FEATURE_REQUEST")));
        efficiencyNode.put("UI_FEEDBACK", calculateAverage(efficiencyScores.get("UI_FEEDBACK")));

        return reportNode;
    }

    private double calculateAverage(List<Double> scores) {
        if (scores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Double s : scores) sum += s;
        // FIX: Linia lipsă a fost adăugată
        double avg = sum / scores.size();
        return Math.round(avg * 100.0) / 100.0;
    }

    private double calculateDays(String assignedAt, String solvedAt) {
        if (assignedAt == null || assignedAt.isEmpty() || solvedAt == null || solvedAt.isEmpty()) {
            return 1.0;
        }
        LocalDate start = LocalDate.parse(assignedAt);
        LocalDate end = LocalDate.parse(solvedAt);
        long days = ChronoUnit.DAYS.between(start, end) + 1; // Inclusiv ultima zi
        return Math.max(1.0, (double) days);
    }

    // --- FORMULE EFICIENTA ---

    private double calculateBugScore(Bug b, double days) {
        // (frequency + severityFactor) * 10 / days
        int f = getFrequencyValue(b.getFrequency());
        int s = getSeverityValue(b.getSeverity());
        return (f + s) * 10.0 / days;
    }

    private double calculateFeatureScore(featureRequest f, double days) {
        // (businessValue + customerDemand) / days
        int v = getBusinessValue(f.getBusinessValue());
        int d = getDemandValue(f.getCustomerDemand());
        return (v + d) / days;
    }

    private double calculateUIScore(UIFeedback u, double days) {
        // (usabilityScore + businessValue) / days
        int v = getBusinessValue(u.getBusinessValue());
        int usb = u.getUsabilityScore() != null ? u.getUsabilityScore() : 0;
        return (usb + v) / days;
    }

    // --- HELPER VALUES (Actualizate 1, 3, 6, 10) ---

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