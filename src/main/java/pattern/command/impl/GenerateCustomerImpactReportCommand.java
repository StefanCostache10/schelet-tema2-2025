package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import pattern.command.Command;
import pattern.strategy.CustomerImpactStrategy;
import pattern.strategy.MetricStrategy;
import repository.Database;
import java.util.List;

public class GenerateCustomerImpactReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GenerateCustomerImpactReportCommand(JsonNode commandNode, List<ObjectNode> outputs, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputs = outputs;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        User user = db.findUserByUsername(username);

        // Verificare permisiuni: doar MANAGER
        if (user == null || user.getRole() != model.enums.Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "generateCustomerImpactReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: required role MANAGER; user role " + (user != null ? user.getRole() : "null") + ".");
            outputs.add(error);
            return;
        }

        // Folosim Strategia pentru calcul
        MetricStrategy strategy = new CustomerImpactStrategy();
        ObjectNode reportData = strategy.calculate(mapper, db);

        // Construim output-ul final
        ObjectNode output = mapper.createObjectNode();
        output.put("command", "generateCustomerImpactReport");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.set("report", reportData);

        outputs.add(output);
    }
}