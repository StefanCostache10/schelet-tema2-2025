package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.ticket.Comment;
import model.enums.Role;
import model.enums.TicketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class AddCommentCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();
    private static final int MIN_COMMENT_LENGTH = 10;

    public AddCommentCommand(final JsonNode node, final List<ObjectNode> out,
                             final ObjectMapper mapper) {
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        int ticketId = commandNode.get("ticketID").asInt();
        Ticket ticket = db.findTicketById(ticketId);
        if (ticket == null) {
            return;
        }

        String username = commandNode.get("username").asText();
        String commentText = commandNode.get("comment").asText();
        String timestamp = commandNode.get("timestamp").asText();
        var user = db.findUserByUsername(username);
        if (user == null) {
            return;
        }

        if (ticket.getReportedBy() == null || ticket.getReportedBy().isEmpty()) {
            addError("addComment", username, "Comments are not allowed on anonymous tickets.",
                    timestamp);
            return;
        }

        if (commentText.length() < MIN_COMMENT_LENGTH) {
            addError("addComment", username, "Comment must be at least 10 characters long.",
                    timestamp);
            return;
        }

        if (user.getRole() == Role.REPORTER) {
            if (ticket.getStatus() == TicketStatus.CLOSED) {
                addError("addComment", username, "Reporters cannot comment on CLOSED tickets.",
                        timestamp);
                return;
            }
            if (!ticket.getReportedBy().equals(username)) {
                addError("addComment", username, "Reporter " + username
                        + " cannot comment on ticket " + ticketId + ".", timestamp);
                return;
            }
        }

        if (user.getRole() == Role.DEVELOPER) {
            if (!username.equals(ticket.getAssignedTo())) {
                addError("addComment", username, "Ticket " + ticketId
                        + " is not assigned to the developer " + username + ".", timestamp);
                return;
            }
        }

        Comment newComment = new Comment(username, commentText, timestamp);
        ticket.getComments().add(newComment);

        String target = user.getRole() == Role.DEVELOPER ? ticket.getReportedBy()
                : ticket.getAssignedTo();
        if (target != null && !target.isEmpty()) {
            var targetUser = db.findUserByUsername(target);
            if (targetUser != null) {
                targetUser.update("New comment on ticket " + ticketId);
            }
        }
    }

    private void addError(final String cmd, final String user, final String msg, final String ts) {
        ObjectNode err = mapper.createObjectNode();
        err.put("command", cmd);
        err.put("username", user);
        err.put("timestamp", ts);
        err.put("error", msg);
        outputList.add(err);
    }
}
