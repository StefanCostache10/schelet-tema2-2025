package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import pattern.command.Command;
import repository.Database;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ViewAssignedTicketsCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public ViewAssignedTicketsCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        List<Ticket> assigned = db.getTickets().stream()
                .filter(t -> t.getAssignedTo().equals(username))
                .sorted(Comparator.comparing(Ticket::getBusinessPriority).reversed() // CRITICAL > LOW
                        .thenComparing(Ticket::getTimestamp)
                        .thenComparing(Ticket::getId))
                .collect(Collectors.toList());

        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewAssignedTickets");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode ticketsArray = result.putArray("assignedTickets");

        for (Ticket t : assigned) {
            ticketsArray.add(mapper.valueToTree(t));
        }
        outputList.add(result);
    }
}