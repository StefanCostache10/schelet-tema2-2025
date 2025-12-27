package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.enums.Role;
import model.user.User;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public class CreateMilestoneCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db;

    public CreateMilestoneCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
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

        // Eroare Rol (fără username în mesaj)
        if (user != null && user.getRole() != Role.MANAGER) {
            ObjectNode errorJson = mapper.createObjectNode();
            errorJson.put("command", "createMilestone");
            errorJson.put("username", username);
            errorJson.put("timestamp", timestamp);
            errorJson.put("error", "The user does not have permission to execute this command: required role MANAGER; user role " + user.getRole() + ".");
            outputList.add(errorJson);
            return;
        }

        try {
            Milestone milestone = mapper.treeToValue(commandNode, Milestone.class);
            milestone.setCreatedAt(timestamp);
            milestone.setCreatedBy(username);

            // Validare: Tichete deja atribuite
            for (Integer ticketId : milestone.getTickets()) {
                Milestone existing = db.findMilestoneForTicket(ticketId);
                if (existing != null) {
                    addError(username, "Tickets " + ticketId + " already assigned to milestone " + existing.getName() + ".", timestamp);
                    return;
                }
            }

            db.addMilestone(milestone);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addError(String username, String errorMessage, String timestamp) {
        ObjectNode errorJson = mapper.createObjectNode();
        errorJson.put("command", "createMilestone");
        errorJson.put("username", username);
        errorJson.put("timestamp", timestamp);
        errorJson.put("error", errorMessage);
        outputList.add(errorJson);
    }
}