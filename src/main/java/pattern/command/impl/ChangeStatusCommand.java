package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.ticketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class ChangeStatusCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ChangeStatusCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node; this.outputList = out; this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) return;

        // Validare: Doar developerul asignat poate schimba statusul (conform Test 8)
        if (!username.equals(ticket.getAssignedTo())) {
            ObjectNode err = mapper.createObjectNode();
            err.put("command", "changeStatus");
            err.put("username", username);
            err.put("timestamp", timestamp);
            err.put("error", "Ticket " + ticketId + " is not assigned to developer " + username + ".");
            outputList.add(err);
            return;
        }

// În metoda execute() din ChangeStatusCommand.java
        if (ticket.getStatus() == model.enums.ticketStatus.IN_PROGRESS) {
            ticket.setStatus(model.enums.ticketStatus.RESOLVED);
            ticket.setSolvedAt(timestamp);

            // Găsim numele milestone-ului pentru istoric
            String milestoneName = db.getMilestones().stream()
                    .filter(m -> m.getTickets().contains(ticket.getId()))
                    .map(model.Milestone::getName)
                    .findFirst()
                    .orElse("None");

            ObjectNode action = mapper.createObjectNode();
            action.put("milestone", milestoneName);
            action.put("user", username);
            action.put("action", "RESOLVED");
            action.put("timestamp", timestamp);
            ticket.addAction(action);
        }
    }
}