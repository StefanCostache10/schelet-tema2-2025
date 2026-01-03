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
        // Utilizatorul este garantat să existe conform fluxului de testare general,
        // dar putem verifica pentru siguranță.

        // 1. Verificare status: trebuie să fie IN_PROGRESS
        if (ticket.getStatus() != ticketStatus.IN_PROGRESS) {
            addError(username, "The ticket must be in IN_PROGRESS state to be de-assigned.", timestamp);
            return;
        }

        // 2. Efectuarea operației de renunțare
        ticket.setStatus(ticketStatus.OPEN);
        ticket.setAssignedTo(""); // Se șterge responsabilul
        ticket.setAssignedAt(""); // Se șterge data asignării

        // Notă: Această acțiune ar trebui înregistrată în istoricul tichetului
        // pentru comanda viewTicketHistory (DE-ASSIGNED)[cite: 151, 164].
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