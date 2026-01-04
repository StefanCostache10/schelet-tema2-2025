package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.ticketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class UndoAssignTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public UndoAssignTicketCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
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

        // PROTECȚIE: Verificăm dacă tichetul există înainte de a-l accesa
        if (ticket == null) {
            // Putem alege să ignorăm comanda sau să dăm o eroare,
            // dar important este să NU crăpăm cu NullPointerException.
            // Conform specificațiilor, "Se garantează că ID-ul există", deci acest caz
            // apare doar din cauza bug-ului anterior de ne-creare a tichetului.
            return;
        }

        if (ticket.getStatus() != ticketStatus.IN_PROGRESS) {
            addError(username, "The ticket must be in IN_PROGRESS state to be de-assigned.", timestamp);
            return;
        }

        // Înregistrăm acțiunea în istoric
        ObjectNode action = mapper.createObjectNode();
        action.put("action", "DE-ASSIGNED");
        action.put("by", username);
        action.put("timestamp", timestamp);
        ticket.addAction(action);

        // Efectuarea operației de renunțare
        ticket.setStatus(ticketStatus.OPEN);
        ticket.setAssignedTo("");
        ticket.setAssignedAt("");
    }

    private void addError(String username, String errorMessage, String timestamp) {
        ObjectNode errorJson = mapper.createObjectNode();
        errorJson.put("command", "undoAssignTicket");
        errorJson.put("username", username);
        errorJson.put("timestamp", timestamp);
        errorJson.put("error", errorMessage);
        outputList.add(errorJson);
    }
}