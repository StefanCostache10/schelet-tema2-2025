package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.Role;
import model.user.User;
import pattern.command.Command;
import repository.Database;
import java.util.List;
import java.util.stream.Collectors;

public final class ViewAssignedTicketsCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ViewAssignedTicketsCommand(final JsonNode commandNode, final List<ObjectNode> outputList,
                                      final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        User user = db.findUserByUsername(username);
        if (user == null) {
            return;
        }

        if (user.getRole() != Role.DEVELOPER) {
            ObjectNode err = mapper.createObjectNode();
            err.put("command", "viewAssignedTickets");
            err.put("username", username);
            err.put("timestamp", timestamp);
            err.put("error", "The user does not have permission to execute this command: required role DEVELOPER; user role " + user.getRole() + ".");
            outputList.add(err);
            return;
        }

        List<Ticket> assigned = db.getTickets().stream()
                .filter(t -> t.getAssignedTo().equals(username))
                .sorted((t1, t2) -> {
                    int pComp = db.getCalculatedPriority(t2, timestamp)
                            .compareTo(db.getCalculatedPriority(t1, timestamp));
                    if (pComp != 0) {
                        return pComp;
                    }
                    int tComp = t1.getTimestamp().compareTo(t2.getTimestamp());
                    if (tComp != 0) {
                        return tComp;
                    }
                    return Integer.compare(t1.getId(), t2.getId());
                })
                .collect(Collectors.toList());

        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewAssignedTickets");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode ticketsArray = result.putArray("assignedTickets");

        for (Ticket t : assigned) {

            ObjectNode tNode = mapper.valueToTree(t);

            tNode.put("businessPriority", db.getCalculatedPriority(t, timestamp).toString());

            tNode.remove("assignedTo");
            tNode.remove("solvedAt");
            tNode.remove("description");
            tNode.remove("expertiseArea");

            ticketsArray.add(tNode);
        }
        outputList.add(result);
    }
}
