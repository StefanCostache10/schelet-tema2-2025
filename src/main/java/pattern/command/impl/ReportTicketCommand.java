package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.Role;
import model.enums.TicketPriority;
import model.enums.TicketType;
import model.user.User;
import pattern.command.Command;
import pattern.factory.TicketFactory;
import repository.Database;

import java.time.LocalDate;
import java.util.List;

public final class ReportTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db;

    public ReportTicketCommand(final JsonNode commandNode, final List<ObjectNode> outputList,
                               final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        if (db.getAppStartDate() == null) {
            db.setAppStartDate(LocalDate.parse(timestamp));
        }

        User user = db.findUserByUsername(username);

        if (user == null) {
            addError(username, "The user " + username + " does not exist.", timestamp);
            return;
        }

        if (user.getRole() != Role.REPORTER) {
            addError(username, "The user " + username
                    + " does not have permission to execute this command: "
                    + "required role REPORTER; user role " + user.getRole(), timestamp);
            return;
        }


        if (!db.isInTestingPhase(timestamp)) {
            addError(username, "Tickets can only be reported during testing phases.", timestamp);
            return;
        }

        try {
            JsonNode paramsNode = commandNode.has("params")
                    ? commandNode.get("params") : commandNode;

            Ticket ticket = TicketFactory.createTicketFromCommand(paramsNode, mapper);
            ticket.setTimestamp(timestamp);

            boolean isAnonymous = ticket.getReportedBy() == null
                    || ticket.getReportedBy().isEmpty();

            if (isAnonymous) {
                if (ticket.getType() != TicketType.BUG) {
                    addError(username,
                            "Anonymous reports are only allowed for tickets of type BUG.",
                            timestamp);
                    return;
                }
                ticket.setBusinessPriority(TicketPriority.LOW);
            }

            db.addTicket(ticket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addError(final String username,
                          final String errorMessage, final String timestamp) {
        ObjectNode errorJson = mapper.createObjectNode();
        errorJson.put("command", "reportTicket");
        errorJson.put("username", username);
        errorJson.put("timestamp", timestamp);
        errorJson.put("error", errorMessage);
        outputList.add(errorJson);
    }
}
