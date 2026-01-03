package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.ticketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class UndoChangeStatusCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public UndoChangeStatusCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
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
        if (ticket == null) return;

        if (!username.equals(ticket.getAssignedTo())) {
            ObjectNode err = mapper.createObjectNode();
            err.put("command", "undoChangeStatus");
            err.put("username", username);
            err.put("timestamp", timestamp);
            err.put("error", "Ticket " + ticketId + " is not assigned to developer " + username + ".");
            outputList.add(err);
            return;
        }

        ticketStatus oldStatus = ticket.getStatus();

        // Logica de inversare a stărilor
        if (oldStatus == ticketStatus.CLOSED) {
            // CLOSED -> RESOLVED
            ticket.setStatus(ticketStatus.RESOLVED);
            ticket.setClosedAt("");
            recordStatusChange(ticket, oldStatus.toString(), ticketStatus.RESOLVED.toString(), username, timestamp);

        } else if (oldStatus == ticketStatus.RESOLVED) {
            // RESOLVED -> IN_PROGRESS
            ticket.setStatus(ticketStatus.IN_PROGRESS);
            ticket.setSolvedAt("");
            recordStatusChange(ticket, oldStatus.toString(), ticketStatus.IN_PROGRESS.toString(), username, timestamp);

        } else if (oldStatus == ticketStatus.IN_PROGRESS) {
            // IN_PROGRESS -> OPEN (De-assign)
            ticket.setStatus(ticketStatus.OPEN);

            // Trecerea în OPEN înseamnă și scoaterea developerului (reversul lui AssignTicket)
            ticket.setAssignedTo("");
            ticket.setAssignedAt("");

            // Pentru această tranziție specifică, înregistrăm 'DE-ASSIGNED', nu 'STATUS_CHANGED'
            ObjectNode action = mapper.createObjectNode();
            action.put("action", "DE-ASSIGNED");
            action.put("by", username);
            action.put("timestamp", timestamp);
            ticket.addAction(action);
        }
    }

    private void recordStatusChange(Ticket ticket, String from, String to, String by, String timestamp) {
        ObjectNode action = mapper.createObjectNode();
        action.put("from", from);
        action.put("to", to);
        action.put("by", by);
        action.put("timestamp", timestamp);
        action.put("action", "STATUS_CHANGED");
        ticket.addAction(action);
    }
}