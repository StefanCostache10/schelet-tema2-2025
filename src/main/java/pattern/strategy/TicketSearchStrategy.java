package pattern.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.enums.*;
import model.ticket.Ticket;
import model.user.Developer;
import model.user.Reporter;
import model.user.User;
import pattern.strategy.SearchStrategy;
import repository.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TicketSearchStrategy implements SearchStrategy {
    @Override
    public List<ObjectNode> search(JsonNode filters, String requesterUsername, ObjectMapper mapper, Database db, String timestamp) {
        // Obținem utilizatorul care face cererea pentru a aplica regulile de vizibilitate
        User user = db.findUserByUsername(requesterUsername);
        List<Ticket> tickets = new ArrayList<>(db.getTickets());

        // --- 0. Aplicare reguli de vizibilitate (Base Visibility) ---
        if (user instanceof Reporter) {
            // Reporter: vede doar tichetele introduse de el
            tickets.removeIf(t -> !t.getReportedBy().equals(requesterUsername));
        } else if (user instanceof Developer) {
            // Developer: vede doar tichetele OPEN din milestone-urile la care este repartizat
            tickets.removeIf(t -> {
                // Elimină dacă nu e OPEN
                if (t.getStatus() != ticketStatus.OPEN) return true;

                // Elimină dacă nu aparține unui milestone unde dev-ul este asignat
                Milestone m = db.findMilestoneForTicket(t.getId());
                return m == null || !m.getAssignedDevs().contains(requesterUsername);
            });
        }
        // Manager: vede tot (nu se aplică filtrări de bază)

        // --- 1. Filtrare după tip ---
        if (filters.has("type")) {
            String typeStr = filters.get("type").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getType().toString().equals(typeStr))
                    .collect(Collectors.toList());
        }

        // --- 2. Filtrare după prioritate ---
        if (filters.has("businessPriority")) {
            String prioStr = filters.get("businessPriority").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getBusinessPriority().toString().equals(prioStr))
                    .collect(Collectors.toList());
        }

        // --- 3. Filtrare după data creării (createdAfter) ---
        if (filters.has("createdAfter")) {
            String dateStr = filters.get("createdAfter").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getTimestamp().compareTo(dateStr) > 0)
                    .collect(Collectors.toList());
        }

        // --- 4. Filtrare după cuvinte cheie (keywords) ---
        List<String> keywords = new ArrayList<>();
        if (filters.has("keywords")) {
            for (JsonNode kw : filters.get("keywords")) {
                keywords.add(kw.asText().toLowerCase());
            }
            List<String> finalKeywords = keywords;
            tickets = tickets.stream()
                    .filter(t -> {
                        String desc = t.getDescription() != null ? t.getDescription() : "";
                        String content = (t.getTitle() + " " + desc).toLowerCase();
                        return finalKeywords.stream().anyMatch(content::contains);
                    })
                    .collect(Collectors.toList());
        }

        // --- 5. Filtrare disponibilitate (availableForAssignment) ---
        if (filters.has("availableForAssignment") && filters.get("availableForAssignment").asBoolean()) {
            if (user instanceof Developer) {
                Developer dev = (Developer) user;
                tickets = tickets.stream()
                        .filter(t -> isAvailableForAssignment(t, dev, db, timestamp))
                        .collect(Collectors.toList());
            }
        }

        // --- Sortare rezultate (CreatedAt ASC, ID ASC) ---
        tickets.sort(Comparator.comparing(Ticket::getTimestamp)
                .thenComparing(Ticket::getId));

        // Construire rezultate JSON
        List<ObjectNode> results = new ArrayList<>();
        for (Ticket t : tickets) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", t.getId());
            node.put("type", t.getType().toString());
            node.put("title", t.getTitle());
            node.put("businessPriority", t.getBusinessPriority().toString());
            node.put("status", t.getStatus().toString());
            node.put("createdAt", t.getTimestamp());
            node.put("solvedAt", t.getSolvedAt());
            node.put("reportedBy", t.getReportedBy());

            // Sortare matchingWords lexicografic
            if (!keywords.isEmpty()) {
                ArrayNode mwNode = node.putArray("matchingWords");
                String desc = t.getDescription() != null ? t.getDescription() : "";
                String content = (t.getTitle() + " " + desc).toLowerCase();

                keywords.stream()
                        .filter(content::contains)
                        .sorted()
                        .forEach(mwNode::add);
            }
            results.add(node);
        }
        return results;
    }

    /**
     * Verifică dacă un tichet poate fi preluat (assigned) de un developer.
     * Aceasta implică reguli suplimentare față de simpla vizibilitate (expertiză, senioritate, milestone blocat).
     * Notă: Condițiile de bază (OPEN, apartenență milestone) sunt deja filtrate mai sus,
     * dar le păstrăm aici pentru completitudine sau în caz de reutilizare.
     */
    private boolean isAvailableForAssignment(Ticket t, Developer dev, Database db, String timestamp) {
        // Trebuie să fie OPEN
        if (t.getStatus() != ticketStatus.OPEN) return false;

        // Nu trebuie să fie deja asignat
        if (t.getAssignedTo() != null && !t.getAssignedTo().isEmpty()) return false;

        // Verificare Expertiză
        List<Expertise> requiredExps = getRequiredSpecializations(t.getExpertiseArea());
        if (!requiredExps.contains(dev.getExpertiseArea())) return false;

        // Verificare Senioritate
        ticketPriority currentP = db.getCalculatedPriority(t, timestamp);
        List<Seniority> requiredSens = getRequiredSeniorities(t.getType(), currentP);
        if (!requiredSens.contains(dev.getSeniority())) return false;

        // Verificare Milestone Blocat
        Milestone m = db.findMilestoneForTicket(t.getId());
        if (m != null && db.isMilestoneBlocked(m)) return false;

        return true;
    }

    private List<Expertise> getRequiredSpecializations(String areaStr) {
        if (areaStr == null) return Collections.emptyList();
        Expertise area = Expertise.valueOf(areaStr);
        List<Expertise> res = new ArrayList<>();
        switch (area) {
            case FRONTEND: res.addAll(Arrays.asList(Expertise.FRONTEND, Expertise.FULLSTACK, Expertise.DESIGN)); break;
            case BACKEND: res.addAll(Arrays.asList(Expertise.BACKEND, Expertise.FULLSTACK)); break;
            case DEVOPS: res.addAll(Arrays.asList(Expertise.DEVOPS, Expertise.FULLSTACK)); break;
            case DESIGN: res.addAll(Arrays.asList(Expertise.DESIGN, Expertise.FRONTEND, Expertise.FULLSTACK)); break;
            case DB: res.addAll(Arrays.asList(Expertise.BACKEND, Expertise.DB, Expertise.FULLSTACK)); break;
            default: res.add(Expertise.FULLSTACK);
        }
        return res;
    }

    private List<Seniority> getRequiredSeniorities(ticketType type, ticketPriority priority) {
        List<Seniority> res = new ArrayList<>();
        if (type == ticketType.FEATURE_REQUEST) {
            if (priority == ticketPriority.CRITICAL) res.add(Seniority.SENIOR);
            else res.addAll(Arrays.asList(Seniority.MID, Seniority.SENIOR));
        } else {
            if (priority == ticketPriority.CRITICAL) res.add(Seniority.SENIOR);
            else if (priority == ticketPriority.HIGH) res.addAll(Arrays.asList(Seniority.MID, Seniority.SENIOR));
            else res.addAll(Arrays.asList(Seniority.JUNIOR, Seniority.MID, Seniority.SENIOR));
        }
        return res;
    }
}