package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pattern.command.Command;
import pattern.strategy.SearchStrategy;
import pattern.strategy.DeveloperSearchStrategy;
import pattern.strategy.TicketSearchStrategy;
import repository.Database;

import java.util.List;

public final class SearchCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public SearchCommand(final JsonNode node,
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
        String searchType = commandNode.get("filters").get("searchType").asText();

        JsonNode filters = commandNode.get("filters");

        SearchStrategy strategy;
        if (searchType.equals("TICKET")) {
            strategy = new TicketSearchStrategy();
        } else {
            strategy = new DeveloperSearchStrategy();
        }

        List<ObjectNode> results = strategy.search(filters, username, mapper, db, timestamp);

        ObjectNode output = mapper.createObjectNode();
        output.put("command", "search");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.put("searchType", searchType);

        ArrayNode resultsArray = output.putArray("results");
        results.forEach(resultsArray::add);

        outputList.add(output);
    }
}
