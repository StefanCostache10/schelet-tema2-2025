package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.ticket.Ticket;
import model.enums.ticketStatus;
import model.enums.Expertise;
import model.enums.Seniority;
import model.enums.ticketPriority;
import model.enums.ticketType;
import model.user.Developer;
import model.user.User;
import pattern.command.Command;
import repository.Database;

import java.util.*;
import java.util.stream.Collectors;

public class AssignTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public AssignTicketCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        int ticketId = commandNode.get("ticketID").asInt();

        Ticket ticket = db.findTicketById(ticketId);

        // FIX: Verificare existență tichet pentru a evita NullPointerException
        if (ticket == null) {
            addError(username, "Ticket " + ticketId + " not found.", timestamp);
            return;
        }

        User user = db.findUserByUsername(username);

        if (!(user instanceof Developer)) return;
        Developer dev = (Developer) user;

        // 1. Validare Expertiză
        List<Expertise> requiredExps = getRequiredSpecializations(ticket.getExpertiseArea());
        if (!requiredExps.contains(dev.getExpertiseArea())) {
            String reqStr = requiredExps.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
            addError(username, "Developer " + username + " cannot assign ticket " + ticketId +
                    " due to expertise area. Required: " + reqStr + "; Current: " + dev.getExpertiseArea() + ".", timestamp);
            return;
        }

        // 2. Validare Senioritate
        ticketPriority currentP = db.getCalculatedPriority(ticket, timestamp);
        List<Seniority> requiredSens = getRequiredSeniorities(ticket.getType(), currentP);
        if (!requiredSens.contains(dev.getSeniority())) {
            String reqStr = requiredSens.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
            addError(username, "Developer " + username + " cannot assign ticket " + ticketId +
                    " due to seniority level. Required: " + reqStr + "; Current: " + dev.getSeniority() + ".", timestamp);
            return;
        }

        // 3. Validare Status
        if (ticket.getStatus() != ticketStatus.OPEN) {
            addError(username, "Only OPEN tickets can be assigned.", timestamp);
            return;
        }

        // 4. Validare Milestone
        Milestone m = db.findMilestoneForTicket(ticketId);
        if (m == null || !m.getAssignedDevs().contains(username)) {
            String mName = (m != null) ? m.getName() : "Unknown";
            addError(username, "Developer " + username + " is not assigned to milestone " + mName + ".", timestamp);
            return;
        }

        // 5. Validare Milestone Blocat
        if (db.isMilestoneBlocked(m)) {
            addError(username, "Cannot assign ticket " + ticketId + " from blocked milestone " + m.getName() + ".", timestamp);
            return;
        }

        // --- APLICARE MODIFICĂRI ȘI ISTORIC ---

        // Salvez statusul vechi pentru istoric
        String oldStatus = ticket.getStatus().toString();

        ticket.setStatus(ticketStatus.IN_PROGRESS);
        ticket.setAssignedTo(username);
        ticket.setAssignedAt(timestamp);

        // Actiunea 1: ASSIGNED
        ObjectNode assignedAction = mapper.createObjectNode();
        assignedAction.put("action", "ASSIGNED");
        assignedAction.put("by", username);
        assignedAction.put("timestamp", timestamp);
        ticket.addAction(assignedAction);

        // Actiunea 2: STATUS_CHANGED (din OPEN in IN_PROGRESS)
        ObjectNode statusAction = mapper.createObjectNode();
        statusAction.put("action", "STATUS_CHANGED");
        statusAction.put("from", oldStatus);
        statusAction.put("to", ticket.getStatus().toString());
        statusAction.put("by", username);
        statusAction.put("timestamp", timestamp);
        ticket.addAction(statusAction);

        // ---------------------------------------

        user.update("Ticket " + ticketId + " has been assigned to you.");
    }

    // ... (restul metodelor private rămân neschimbate)
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

    private void addError(String user, String msg, String ts) {
        ObjectNode err = mapper.createObjectNode();
        err.put("command", "assignTicket");
        err.put("username", user);
        err.put("timestamp", ts);
        err.put("error", msg);
        outputList.add(err);
    }
}