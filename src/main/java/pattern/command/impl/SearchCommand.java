package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pattern.command.Command;
import pattern.strategy.SearchStrategy;
import pattern.strategy.impl.DeveloperSearchStrategy;
import pattern.strategy.impl.TicketSearchStrategy;
import repository.Database;

import java.util.List;

public class SearchCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public SearchCommand(JsonNode node, List<ObjectNode> out, ObjectMapper mapper) {
        this.commandNode = node;
        this.outputList = out;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        JsonNode filters = commandNode.get("filters");

        String searchType = "TICKET";
        if (filters.has("searchType")) {
            searchType = filters.get("searchType").asText();
        }

        SearchStrategy strategy;
        if ("DEVELOPER".equals(searchType)) {
            strategy = new DeveloperSearchStrategy();
        } else {
            strategy = new TicketSearchStrategy();
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