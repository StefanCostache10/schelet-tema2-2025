package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.ticket.Comment;
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
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) return;

        String username = commandNode.get("username").asText();
        String commentText = commandNode.get("comment").asText();
        String timestamp = commandNode.get("timestamp").asText();
        var user = db.findUserByUsername(username);
        if (user == null) return;

        // 1. Validare tichet anonim (Prioritate ridicată)
        if (ticket.getReportedBy() == null || ticket.getReportedBy().isEmpty()) {
            addError("addComment", username, "Comments are not allowed on anonymous tickets.", timestamp);
            return;
        }

        // 2. Validare LUNGIME (Trebuie să fie înainte de validarea rolului conform Test 7)
        if (commentText.length() < 10) {
            addError("addComment", username, "Comment must be at least 10 characters long.", timestamp);
            return;
        }

        // 3. Validări specifice pentru roluri (Mesaje dinamice conform referinței)
        if (user.getRole() == Role.REPORTER) {
            if (ticket.getStatus() == ticketStatus.CLOSED) {
                addError("addComment", username, "Reporters cannot comment on CLOSED tickets.", timestamp);
                return;
            }
            if (!ticket.getReportedBy().equals(username)) {
                addError("addComment", username, "Reporter " + username + " cannot comment on ticket " + ticketId + ".", timestamp);
                return;
            }
        }

        if (user.getRole() == Role.DEVELOPER) {
            if (!username.equals(ticket.getAssignedTo())) {
                addError("addComment", username, "Ticket " + ticketId + " is not assigned to the developer " + username + ".", timestamp);
                return;
            }
        }

        // Adăugare comentariu
        Comment newComment = new Comment(username, commentText, timestamp);
        ticket.getComments().add(newComment);

        // Notificare Observer
        String target = user.getRole() == Role.DEVELOPER ? ticket.getReportedBy() : ticket.getAssignedTo();
        if (target != null && !target.isEmpty()) {
            var targetUser = db.findUserByUsername(target);
            if (targetUser != null) targetUser.update("New comment on ticket " + ticketId);
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