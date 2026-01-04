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

public class GenerateTicketRiskReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GenerateTicketRiskReportCommand(JsonNode commandNode, List<ObjectNode> outputs, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputs = outputs;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        // 1. Verificare User
        User user = db.findUserByUsername(username);
        if (user == null) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generateTicketRiskReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "User not found."); // Mesaj generic, ajustează dacă e specificat altfel
            outputs.add(error);
            return;
        }

        // 2. Verificare Permisiuni (Manager)
        if (user.getRole() != model.enums.Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generateTicketRiskReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: required role MANAGER; user role " + user.getRole() + ".");
            outputs.add(error);
            return;
        }

        // 3. Executare Strategie
        MetricStrategy strategy = new TicketRiskStrategy();
        ObjectNode reportData = strategy.calculate(mapper, db);

        // 4. Output
        ObjectNode output = mapper.createObjectNode();
        output.put("command", "generateTicketRiskReport");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.set("report", reportData);

        outputs.add(output);
    }
}