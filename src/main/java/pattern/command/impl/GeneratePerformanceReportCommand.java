package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import model.enums.Role;
import pattern.command.Command;
import pattern.strategy.PerformanceStrategy;
import repository.Database;

import java.util.List;

public final class GeneratePerformanceReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GeneratePerformanceReportCommand(final JsonNode commandNode,
                                            final List<ObjectNode> outputs,
                                            final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputs = outputs;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        db.updateCurrentDate(timestamp);

        User user = db.findUserByUsername(username);
        if (user == null || user.getRole() != Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generatePerformanceReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: "
                    + "required role MANAGER; user role "
                    + (user != null ? user.getRole() : "null") + ".");
            outputs.add(error);
            return;
        }

        PerformanceStrategy strategy = new PerformanceStrategy(username, timestamp);
        ObjectNode wrapper = strategy.calculate(mapper, db);
        JsonNode reportArray = wrapper.get("result");

        ObjectNode output = mapper.createObjectNode();
        output.put("command", "generatePerformanceReport");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.set("report", reportArray);

        outputs.add(output);
    }
}
