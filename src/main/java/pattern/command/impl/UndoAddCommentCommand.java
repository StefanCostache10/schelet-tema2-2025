package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.ticket.Comment;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class UndoAddCommentCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public UndoAddCommentCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node; this.outputList = out; this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) return;

        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        if (ticket.getReportedBy() == null || ticket.getReportedBy().isEmpty()) {
            addError(username, "Cannot remove comment from an anonymous ticket.", timestamp);
            return;
        }

        // Găsim ultimul comentariu al utilizatorului
        List<Comment> comments = ticket.getComments();
        for (int i = comments.size() - 1; i >= 0; i--) {
            if (comments.get(i).getAuthor().equals(username)) {
                comments.remove(i);
                return; // Am șters ultimul comentariu, ieșim
            }
        }
    }

    private void addError(String user, String msg, String ts) {
        ObjectNode err = mapper.createObjectNode();
        err.put("command", "undoAddComment");
        err.put("username", user);
        err.put("timestamp", ts);
        err.put("error", msg);
        outputList.add(err);
    }
}