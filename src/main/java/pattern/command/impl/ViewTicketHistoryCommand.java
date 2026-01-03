package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.ticket.Comment;
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

        // --- Logica de filtrare extinsă pentru a include tichetele unde userul a avut activitate ---
        List<Ticket> ticketsToView = db.getTickets().stream()
                .filter(t -> isRelevantForUser(t, username))
                .collect(Collectors.toList());
        // ------------------------------------------------------------------------------------------

        for (Ticket ticket : ticketsToView) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", ticket.getId());
            tNode.put("title", ticket.getTitle());
            tNode.put("status", ticket.getStatus().toString());

            ArrayNode actionsArray = tNode.putArray("actions");
            if (ticket.getActions() != null) {
                ticket.getActions().forEach(actionsArray::add);
            }

            // Corectat cheile pentru comentarii
            ArrayNode commentsArray = tNode.putArray("comments");
            if (ticket.getComments() != null) {
                for (Comment c : ticket.getComments()) {
                    ObjectNode cNode = mapper.createObjectNode();
                    cNode.put("author", c.getAuthor());
                    cNode.put("content", c.getContent());      // Corectat: 'text' -> 'content'
                    cNode.put("createdAt", c.getCreatedAt());  // Corectat: 'timestamp' -> 'createdAt'
                    commentsArray.add(cNode);
                }
            }

            historyArray.add(tNode);
        }

        outputList.add(result);
    }

    // Metoda ajutătoare pentru a determina relevanța unui tichet
    private boolean isRelevantForUser(Ticket t, String username) {
        // Dacă este reporter sau assignee curent
        if (username.equals(t.getReportedBy()) || username.equals(t.getAssignedTo())) {
            return true;
        }
        // Dacă userul apare în istoricul de acțiuni (a lucrat la el în trecut)
        if (t.getActions() != null) {
            for (JsonNode action : t.getActions()) {
                if (action.has("by") && action.get("by").asText().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }
}