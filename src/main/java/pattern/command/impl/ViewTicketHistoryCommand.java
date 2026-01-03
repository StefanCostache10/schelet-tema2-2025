package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import pattern.command.Command;
import repository.Database;
import java.util.List;
import java.util.stream.Collectors;

public class ViewTicketHistoryCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ViewTicketHistoryCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node; this.outputList = out; this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewTicketHistory");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode historyArray = result.putArray("ticketHistory");

        List<Ticket> ticketsToView;

        // Verificăm dacă avem un ticketID specific sau vizualizăm tot ce e asignat user-ului
        if (commandNode.has("ticketID")) {
            int ticketId = commandNode.get("ticketID").asInt();
            ticketsToView = db.getTickets().stream()
                    .filter(t -> t.getId() == ticketId)
                    .collect(Collectors.toList());
        } else {
            ticketsToView = db.getTickets().stream()
                    .filter(t -> username.equals(t.getAssignedTo()))
                    .collect(Collectors.toList());
        }

        for (Ticket ticket : ticketsToView) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", ticket.getId());
            tNode.put("title", ticket.getTitle());
            tNode.put("status", ticket.getStatus().toString());

            ArrayNode actionsArray = tNode.putArray("actions");
            for (ObjectNode action : ticket.getActions()) {
                actionsArray.add(action);
            }
            historyArray.add(tNode);
        }

        outputList.add(result);
    }
}