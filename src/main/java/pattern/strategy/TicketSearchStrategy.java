package pattern.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.enums.Expertise;
import model.enums.Role;
import model.enums.Seniority;
import model.enums.TicketPriority;
import model.enums.TicketStatus;
import model.enums.TicketType;
import model.ticket.Ticket;
import model.user.Developer;
import model.user.Reporter;
import model.user.User;
import repository.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class TicketSearchStrategy implements SearchStrategy {
    @Override
    public List<ObjectNode> search(final JsonNode filters, final String requesterUsername,
                                   final ObjectMapper mapper, final Database db,
                                   final String timestamp) {
        User user = db.findUserByUsername(requesterUsername);
        List<Ticket> tickets = new ArrayList<>(db.getTickets());

        // 1. Filtrare vizibilitate în funcție de rol
        if (user instanceof Reporter) {
            tickets.removeIf(t -> !t.getReportedBy().equals(requesterUsername));
        } else if (user instanceof Developer) {
            tickets.removeIf(t -> {
                if (t.getStatus() != TicketStatus.OPEN) {
                    return true;
                }
                Milestone m = db.findMilestoneForTicket(t.getId());
                return m == null || !m.getAssignedDevs().contains(requesterUsername);
            });
        }

        // 2. Aplicare filtre din cerere
        if (filters.has("type")) {
            String typeStr = filters.get("type").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getType().toString().equals(typeStr))
                    .collect(Collectors.toList());
        }

        if (filters.has("businessPriority")) {
            String prioStr = filters.get("businessPriority").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getBusinessPriority().toString().equals(prioStr))
                    .collect(Collectors.toList());
        }

        if (filters.has("createdAfter")) {
            String dateStr = filters.get("createdAfter").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getTimestamp().compareTo(dateStr) > 0)
                    .collect(Collectors.toList());
        }

        if (filters.has("createdBefore")) {
            String dateStr = filters.get("createdBefore").asText();
            tickets = tickets.stream()
                    .filter(t -> t.getTimestamp().compareTo(dateStr) < 0)
                    .collect(Collectors.toList());
        }

        // Procesare keywords
        List<String> keywords = new ArrayList<>();
        if (filters.has("keywords")) {
            for (JsonNode kw : filters.get("keywords")) {
                keywords.add(kw.asText().toLowerCase());
            }
            if (!keywords.isEmpty()) {
                List<String> finalKeywords = keywords;
                tickets = tickets.stream()
                        .filter(t -> {
                            String desc = t.getDescription() != null ? t.getDescription() : "";
                            String content = (t.getTitle() + " " + desc).toLowerCase();
                            return finalKeywords.stream().anyMatch(content::contains);
                        })
                        .collect(Collectors.toList());
            }
        }

        if (filters.has("availableForAssignment")
                && filters.get("availableForAssignment").asBoolean()) {
            if (user instanceof Developer) {
                Developer dev = (Developer) user;
                tickets = tickets.stream()
                        .filter(t -> isAvailableForAssignment(t, dev, db, timestamp))
                        .collect(Collectors.toList());
            }
        }

        // 3. Sortare (după timestamp, apoi ID)
        tickets.sort(Comparator.comparing(Ticket::getTimestamp)
                .thenComparing(Ticket::getId));

        // 4. Construire rezultate JSON
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

            // FIX: Adăugăm matchingWords dacă avem keywords SAU dacă userul este MANAGER.
            boolean hasKeywords = filters.has("keywords");

            if (hasKeywords || user.getRole() == Role.MANAGER) {
                ArrayNode mwNode = node.putArray("matchingWords");
                if (!keywords.isEmpty()) {
                    String desc = t.getDescription() != null ? t.getDescription() : "";
                    String content = (t.getTitle() + " " + desc).toLowerCase();

                    keywords.stream()
                            .filter(content::contains)
                            .distinct()
                            .sorted()
                            .forEach(mwNode::add);
                }
            }
            results.add(node);
        }
        return results;
    }

    /**
     * Verifică dacă un tichet poate fi preluat (assigned) de un developer.
     * Aceasta implică reguli suplimentare față de simpla vizibilitate
     * (expertiză, senioritate, milestone blocat).
     * Notă: Condițiile de bază (OPEN, apartenență milestone) sunt deja filtrate mai sus,
     * dar le păstrăm aici pentru completitudine sau în caz de reutilizare.
     */
    private boolean isAvailableForAssignment(final Ticket t, final Developer dev,
                                             final Database db, final String timestamp) {
        if (t.getStatus() != TicketStatus.OPEN) {
            return false;
        }

        if (t.getAssignedTo() != null && !t.getAssignedTo().isEmpty()) {
            return false;
        }

        List<Expertise> requiredExps = getRequiredSpecializations(t.getExpertiseArea());
        if (!requiredExps.contains(dev.getExpertiseArea())) {
            return false;
        }

        TicketPriority currentP = db.getCalculatedPriority(t, timestamp);
        List<Seniority> requiredSens = getRequiredSeniorities(t.getType(), currentP);
        if (!requiredSens.contains(dev.getSeniority())) {
            return false;
        }

        Milestone m = db.findMilestoneForTicket(t.getId());
        if (m != null && db.isMilestoneBlocked(m)) {
            return false;
        }

        return true;
    }

    private List<Expertise> getRequiredSpecializations(final String areaStr) {
        if (areaStr == null) {
            return Collections.emptyList();
        }
        Expertise area = Expertise.valueOf(areaStr);
        List<Expertise> res = new ArrayList<>();
        switch (area) {
            case FRONTEND:
                res.addAll(Arrays.asList(Expertise.FRONTEND,
                        Expertise.FULLSTACK, Expertise.DESIGN));
                break;
            case BACKEND:
                res.addAll(Arrays.asList(Expertise.BACKEND, Expertise.FULLSTACK));
                break;
            case DEVOPS:
                res.addAll(Arrays.asList(Expertise.DEVOPS, Expertise.FULLSTACK));
                break;
            case DESIGN:
                res.addAll(Arrays.asList(Expertise.DESIGN,
                        Expertise.FRONTEND, Expertise.FULLSTACK));
                break;
            case DB:
                res.addAll(Arrays.asList(Expertise.BACKEND, Expertise.DB, Expertise.FULLSTACK));
                break;
            default:
                res.add(Expertise.FULLSTACK);
        }
        return res;
    }

    private List<Seniority> getRequiredSeniorities(final TicketType type,
                                                   final TicketPriority priority) {
        List<Seniority> res = new ArrayList<>();
        if (type == TicketType.FEATURE_REQUEST) {
            if (priority == TicketPriority.CRITICAL) {
                res.add(Seniority.SENIOR);
            } else {
                res.addAll(Arrays.asList(Seniority.MID, Seniority.SENIOR));
            }
        } else {
            if (priority == TicketPriority.CRITICAL) {
                res.add(Seniority.SENIOR);
            } else if (priority == TicketPriority.HIGH) {
                res.addAll(Arrays.asList(Seniority.MID, Seniority.SENIOR));
            } else {
                res.addAll(Arrays.asList(Seniority.JUNIOR, Seniority.MID, Seniority.SENIOR));
            }
        }
        return res;
    }
}
