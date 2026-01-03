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

        // 1. Filtrare și sortare folosind PRIORITATEA CALCULATĂ
        List<Ticket> assigned = db.getTickets().stream()
                .filter(t -> t.getAssignedTo().equals(username))
                .sorted((t1, t2) -> {
                    // Prioritate (CRITICAL > LOW)
                    int pComp = db.getCalculatedPriority(t2, timestamp)
                            .compareTo(db.getCalculatedPriority(t1, timestamp));
                    if (pComp != 0) return pComp;
                    // CreatedAt crescător
                    int tComp = t1.getTimestamp().compareTo(t2.getTimestamp());
                    if (tComp != 0) return tComp;
                    // ID crescător
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

            // 2. ACTUALIZARE PRIORITATE în output
            tNode.put("businessPriority", db.getCalculatedPriority(t, timestamp).toString());

            // 3. ELIMINARE câmpuri extra conform ref-ului pentru viewAssignedTickets
            tNode.remove("assignedTo");
            tNode.remove("solvedAt");

            ticketsArray.add(tNode);
        }
        outputList.add(result);
    }
}