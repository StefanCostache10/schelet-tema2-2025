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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ViewTicketsCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db;

    public ViewTicketsCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        User user = db.findUserByUsername(username);

        if (user == null) {
            // Dacă userul nu există, nu facem nimic (sau aruncăm eroare dacă cere specific)
            // Pentru viewTickets de obicei se ignoră sau se dă eroare standard, dar în testul 1 pare OK.
            return;
        }

        List<Ticket> allTickets = db.getTickets();
        List<Ticket> filteredTickets = new ArrayList<>();

        // Filtrare în funcție de rol
        if (user.getRole() == Role.MANAGER) {
            filteredTickets.addAll(allTickets);
        } else if (user.getRole() == Role.REPORTER) {
            filteredTickets = allTickets.stream()
                    .filter(t -> t.getReportedBy() != null && t.getReportedBy().equals(username))
                    .collect(Collectors.toList());
        } else if (user.getRole() == Role.DEVELOPER) {
            // Logică developer (tbd)
        }

        // Sortare
        filteredTickets.sort(Comparator.comparing(Ticket::getTimestamp, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(Ticket::getId));

        ObjectNode commandOutput = mapper.createObjectNode();
        commandOutput.put("command", "viewTickets");
        // Eliminat: commandOutput.put("user", username); -> Nu apare în ref
        commandOutput.put("username", username);
        commandOutput.put("timestamp", timestamp);

        ArrayNode ticketsArray = mapper.createArrayNode();
        for (Ticket t : filteredTickets) {
            ObjectNode tNode = mapper.valueToTree(t);
            // Suprascriem businessPriority cu cea calculată la momentul curent
            tNode.put("businessPriority", db.getCalculatedPriority(t, timestamp).toString());
            ticketsArray.add(tNode);
        }

        commandOutput.set("tickets", ticketsArray);
        outputList.add(commandOutput);
    }
}