package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.TicketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class ChangeStatusCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ChangeStatusCommand(final JsonNode node, final List<ObjectNode> out,
                               final ObjectMapper mapper) {
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) {
            return;
        }

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }

        if (!username.equals(ticket.getAssignedTo())) {
            ObjectNode err = mapper.createObjectNode();
            err.put("command", "changeStatus");
            err.put("username", username);
            err.put("timestamp", timestamp);
            err.put("error", "Ticket " + ticketId + " is not assigned to developer "
                    + username + ".");
            outputList.add(err);
            return;
        }

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = null;

        if (oldStatus == TicketStatus.IN_PROGRESS) {
            newStatus = TicketStatus.RESOLVED;
            ticket.setStatus(newStatus);
            ticket.setSolvedAt(timestamp);
        } else if (oldStatus == TicketStatus.RESOLVED) {
            newStatus = TicketStatus.CLOSED;
            ticket.setStatus(newStatus);
            ticket.setClosedAt(timestamp);
        }

        if (newStatus != null) {
            ObjectNode action = mapper.createObjectNode();
            action.put("from", oldStatus.toString());
            action.put("to", newStatus.toString());
            action.put("by", username);
            action.put("timestamp", timestamp);
            action.put("action", "STATUS_CHANGED");
            ticket.addAction(action);

            if (newStatus == TicketStatus.CLOSED) {
                db.checkDependenciesAfterClosingTicket(ticket);
            }
        }
    }
}
