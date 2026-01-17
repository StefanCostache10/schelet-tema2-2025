package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.Role;
import model.enums.TicketStatus;
import pattern.command.Command;
import repository.Database;
import java.util.List;

public final class StartTestingPhaseCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db = Database.getInstance();

    public StartTestingPhaseCommand(final JsonNode commandNode, final List<ObjectNode> outputList,
                                    final ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        if (db.findUserByUsername(username) == null
                || db.findUserByUsername(username).getRole() != Role.MANAGER) {
            return;
        }

        boolean hasActive = db.getMilestones().stream()
                .anyMatch(m -> m.getTickets().stream()
                        .map(db::findTicketById)
                        .anyMatch(t -> t != null && t.getStatus() != TicketStatus.CLOSED));

        if (hasActive) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "startTestingPhase");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error",
                    "Cannot start a new testing phase while there are active milestones.");
            outputList.add(error);
            return;
        }

        db.startNewTestingPhase(timestamp);
    }
}
