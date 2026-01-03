package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.ticket.Comment; // Asigură-te că ai o clasă Comment (vezi mai jos)
import model.enums.Role;
import model.enums.ticketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class AddCommentCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public AddCommentCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node; this.outputList = out; this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) return; // Ignorăm conform cerinței

        String username = commandNode.get("username").asText();
        String commentText = commandNode.get("comment").asText();
        String timestamp = commandNode.get("timestamp").asText();
        var user = db.findUserByUsername(username);

        // Validări
        if (ticket.getReportedBy() == null || ticket.getReportedBy().isEmpty()) {
            addError("addComment", username, "Cannot add comment to an anonymous ticket.", timestamp);
            return;
        }
        if (user.getRole() == Role.REPORTER) {
            if (ticket.getStatus() == ticketStatus.CLOSED) {
                addError("addComment", username, "Reporters cannot comment on CLOSED tickets.", timestamp);
                return;
            }
            if (!ticket.getReportedBy().equals(username)) {
                addError("addComment", username, "Reporters can only comment on their own tickets.", timestamp);
                return;
            }
        }
        if (user.getRole() == Role.DEVELOPER) {
            if (!username.equals(ticket.getAssignedTo())) {
                addError("addComment", username, "Developers can only comment on their assigned tickets.", timestamp);
                return;
            }
        }
        if (commentText.length() < 10) {
            addError("addComment", username, "Comment must be at least 10 characters long.", timestamp);
            return;
        }

        // Adăugare comentariu
        Comment newComment = new Comment(username, commentText, timestamp);
        ticket.getComments().add(newComment);

        // Notificare Observer (Dacă e developer, notificăm reporterul și invers)
        String target = user.getRole() == Role.DEVELOPER ? ticket.getReportedBy() : ticket.getAssignedTo();
        if (target != null && !target.isEmpty()) {
            db.findUserByUsername(target).update("New comment on ticket " + ticketId);
        }
    }

    private void addError(String cmd, String user, String msg, String ts) {
        ObjectNode err = mapper.createObjectNode();
        err.put("command", cmd);
        err.put("username", user);
        err.put("timestamp", ts);
        err.put("error", msg);
        outputList.add(err);
    }
}