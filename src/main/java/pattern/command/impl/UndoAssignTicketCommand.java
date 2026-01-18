package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.TicketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class UndoAssignTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public UndoAssignTicketCommand(final JsonNode commandNode, final List<ObjectNode> outputList,
                                   final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        int ticketId = commandNode.get("ticketID").asInt();

        Ticket ticket = db.findTicketById(ticketId);

        if (ticket == null) {
            return;
        }

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            addError(username,
                    "Only IN_PROGRESS tickets can be unassigned.",
                    timestamp);
            return;
        }

        ObjectNode action = mapper.createObjectNode();
        action.put("action", "DE-ASSIGNED");
        action.put("by", username);
        action.put("timestamp", timestamp);
        ticket.addAction(action);

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedTo("");
        ticket.setAssignedAt("");
    }

    private void addError(final String username,
                          final String errorMessage,
                          final String timestamp) {
        ObjectNode errorJson = mapper.createObjectNode();
        errorJson.put("command", "undoAssignTicket");
        errorJson.put("username", username);
        errorJson.put("timestamp", timestamp);
        errorJson.put("error", errorMessage);
        outputList.add(errorJson);
    }
}
