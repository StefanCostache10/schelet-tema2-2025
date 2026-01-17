package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.TicketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class UndoChangeStatusCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public UndoChangeStatusCommand(final JsonNode node,
                                   final List<ObjectNode> out,
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

        if (!username.equals(ticket.getAssignedTo())) {
            ObjectNode err = mapper.createObjectNode();
            err.put("command", "undoChangeStatus");
            err.put("username", username);
            err.put("timestamp", timestamp);
            err.put("error", "Ticket " + ticketId
                    + " is not assigned to developer "
                    + username + ".");
            outputList.add(err);
            return;
        }

        TicketStatus oldStatus = ticket.getStatus();

        if (oldStatus == TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setClosedAt("");
            recordStatusChange(ticket,
                    oldStatus.toString(), TicketStatus.RESOLVED.toString(),
                    username, timestamp);

        } else if (oldStatus == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setSolvedAt("");
            recordStatusChange(ticket, oldStatus.toString(),
                    TicketStatus.IN_PROGRESS.toString(),
                    username, timestamp);

        } else if (oldStatus == TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.OPEN);

            ticket.setAssignedTo("");
            ticket.setAssignedAt("");

            ObjectNode action = mapper.createObjectNode();
            action.put("action", "DE-ASSIGNED");
            action.put("by", username);
            action.put("timestamp", timestamp);
            ticket.addAction(action);
        }
    }

    private void recordStatusChange(final Ticket ticket, final String from, final String to,
                                    final String by, final String timestamp) {
        ObjectNode action = mapper.createObjectNode();
        action.put("from", from);
        action.put("to", to);
        action.put("by", by);
        action.put("timestamp", timestamp);
        action.put("action", "STATUS_CHANGED");
        ticket.addAction(action);
    }
}
