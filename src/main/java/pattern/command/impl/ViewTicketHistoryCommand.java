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

public final class ViewTicketHistoryCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ViewTicketHistoryCommand(final JsonNode node,
                                    final List<ObjectNode> out,
                                    final ObjectMapper mapper) {
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        // Corecție: Folosim findUserByUsername în loc de getUser
        boolean isDeveloper = db.findUserByUsername(username).getRole().toString().equals("DEVELOPER");

        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewTicketHistory");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode historyArray = result.putArray("ticketHistory");

        List<Ticket> ticketsToView = db.getTickets().stream()
                .filter(t -> isRelevantForUser(t, username))
                .collect(Collectors.toList());

        for (Ticket ticket : ticketsToView) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", ticket.getId());
            tNode.put("title", ticket.getTitle());
            tNode.put("status", ticket.getStatus().toString());

            String renounceTimestamp = null;
            if (isDeveloper && ticket.getActions() != null) {
                for (JsonNode action : ticket.getActions()) {
                    if (action.get("action").asText().equals("DE-ASSIGNED")
                            && action.get("by").asText().equals(username)) {
                        renounceTimestamp = action.get("timestamp").asText();
                        break;
                    }
                }
            }

            ArrayNode actionsArray = tNode.putArray("actions");
            if (ticket.getActions() != null) {
                for (JsonNode action : ticket.getActions()) {
                    if (renounceTimestamp == null
                            || action.get("timestamp").asText().compareTo(renounceTimestamp) <= 0) {
                        actionsArray.add(action);
                    }
                }
            }

            ArrayNode commentsArray = tNode.putArray("comments");
            if (ticket.getComments() != null) {
                for (Comment c : ticket.getComments()) {
                    if (renounceTimestamp == null
                            || c.getCreatedAt().compareTo(renounceTimestamp) <= 0) {
                        ObjectNode cNode = mapper.createObjectNode();
                        cNode.put("author", c.getAuthor());
                        cNode.put("content", c.getContent());
                        cNode.put("createdAt", c.getCreatedAt());
                        commentsArray.add(cNode);
                    }
                }
            }

            historyArray.add(tNode);
        }

        outputList.add(result);
    }

    private boolean isRelevantForUser(final Ticket t, final String username) {
        if (username.equals(t.getReportedBy()) || username.equals(t.getAssignedTo())) {
            return true;
        }
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
