package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class ViewNotificationsCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;

    public ViewNotificationsCommand(final JsonNode commandNode, final List<ObjectNode> outputs,
                                    final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputs = outputs;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();
        User user = Database.getInstance().findUserByUsername(username);

        if (user != null) {
            ObjectNode outputNode = mapper.createObjectNode();
            outputNode.put("command", "viewNotifications");
            outputNode.put("username", username);
            outputNode.put("timestamp", timestamp);

            List<String> notifications = user.getNotifications();
            ArrayNode notificationsArray = outputNode.putArray("notifications");
            for (String notif : notifications) {
                notificationsArray.add(notif);
            }

            outputs.add(outputNode);

            user.clearNotifications();
        }
    }
}
