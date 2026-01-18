package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import pattern.command.Command;
import pattern.strategy.MetricStrategy;
import pattern.strategy.TicketRiskStrategy;
import repository.Database;

import java.util.List;

public final class GenerateTicketRiskReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GenerateTicketRiskReportCommand(final JsonNode commandNode,
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
        if (user == null) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generateTicketRiskReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "User not found.");
            outputs.add(error);
            return;
        }

        if (user.getRole() != model.enums.Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generateTicketRiskReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: "
                    + "required role MANAGER; user role " + user.getRole() + ".");
            outputs.add(error);
            return;
        }

        MetricStrategy strategy = new TicketRiskStrategy();
        ObjectNode reportData = strategy.calculate(mapper, db);

        ObjectNode output = mapper.createObjectNode();
        output.put("command", "generateTicketRiskReport");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.set("report", reportData);

        outputs.add(output);
    }
}
