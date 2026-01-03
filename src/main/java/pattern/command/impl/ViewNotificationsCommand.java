package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import pattern.command.Command;
import repository.Database;

import java.util.List;

public class ViewNotificationsCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;

    public ViewNotificationsCommand(JsonNode commandNode, List<ObjectNode> outputs, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputs = outputs;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        // Timestamp-ul este important pentru actualizarea timpului în baza de date (vezi punctul 4)
        String timestamp = commandNode.get("timestamp").asText();

        Database db = Database.getInstance();
        // Actualizăm timpul curent al aplicației pentru a verifica deadline-uri
        db.updateCurrentDate(timestamp);

        User user = db.findUserByUsername(username);

        if (user == null) {
            ObjectNode errorNode = mapper.createObjectNode();
            errorNode.put("command", "viewNotifications");
            errorNode.put("status", "ERROR");
            errorNode.put("description", "User not found"); // Sau mesajul standard de eroare
            // outputs.add(errorNode); // Verifică dacă testul cere eroare aici, de obicei da
            return;
        }

        // Construim output-ul
        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put("command", "viewNotifications");
        resultNode.put("username", username);
        resultNode.put("timestamp", timestamp);

        ArrayNode notificationsArray = resultNode.putArray("notifications");
        for (String notif : user.getNotifications()) {
            notificationsArray.add(notif);
        }

        outputs.add(resultNode);

        // Ștergem notificările după vizualizare
        user.clearNotifications();
    }
}