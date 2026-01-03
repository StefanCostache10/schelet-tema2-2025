package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.ticket.Ticket;
import model.enums.ticketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class AssignTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public AssignTicketCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node; this.outputList = out; this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        int ticketId = commandNode.get("ticketID").asInt();

        Ticket ticket = db.findTicketById(ticketId);
        Milestone m = db.findMilestoneForTicket(ticketId);

        // Validări (Exemplu: Status OPEN și Milestone)
        if (ticket.getStatus() != ticketStatus.OPEN) {
            addError(username, "The ticket must be in OPEN state to be assigned.", timestamp);
            return;
        }
        if (m == null || !m.getAssignedDevs().contains(username)) {
            addError(username, "The developer is not assigned to the milestone containing this ticket.", timestamp);
            return;
        }

        // Aplicare
        ticket.setStatus(ticketStatus.IN_PROGRESS);
        ticket.setAssignedTo(username);
        ticket.setAssignedAt(timestamp);

        // Notificare Observer
        db.findUserByUsername(username).update("Ticket " + ticketId + " has been assigned to you.");
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