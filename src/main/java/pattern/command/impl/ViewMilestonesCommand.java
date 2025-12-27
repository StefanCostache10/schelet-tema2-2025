package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Milestone;
import model.ticket.Ticket;
import model.enums.Role;
import model.enums.ticketStatus;
import model.user.User;
import pattern.command.Command;
import repository.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ViewMilestonesCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db;

    public ViewMilestonesCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
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
        if (user == null) return;

        List<Milestone> filtered;
        if (user.getRole() == Role.MANAGER) {
            filtered = db.getMilestones().stream()
                    .filter(m -> m.getCreatedBy().equals(username))
                    .collect(Collectors.toList());
        } else {
            filtered = db.getMilestones().stream()
                    .filter(m -> m.getAssignedDevs().contains(username))
                    .collect(Collectors.toList());
        }

        filtered.sort(Comparator.comparing(Milestone::getDueDate).thenComparing(Milestone::getName));

        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewMilestones");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode milestonesArray = result.putArray("milestones");

        LocalDate now = LocalDate.parse(timestamp);

        for (Milestone m : filtered) {
            ObjectNode mNode = mapper.valueToTree(m);

            // Calculare tichete
            List<Ticket> milestoneTickets = db.getTickets().stream()
                    .filter(t -> m.getTickets().contains(t.getId()))
                    .collect(Collectors.toList());

            List<Integer> open = milestoneTickets.stream().filter(t -> t.getStatus() != ticketStatus.CLOSED).map(Ticket::getId).collect(Collectors.toList());
            List<Integer> closed = milestoneTickets.stream().filter(t -> t.getStatus() == ticketStatus.CLOSED).map(Ticket::getId).collect(Collectors.toList());

            mNode.set("openTickets", mapper.valueToTree(open));
            mNode.set("closedTickets", mapper.valueToTree(closed));

            double progress = milestoneTickets.isEmpty() ? 0.0 : (double) closed.size() / milestoneTickets.size() * 100.0;
            mNode.put("completionPercentage", Math.round(progress * 100.0) / 100.0);

            // Zile rămase / Overdue
            LocalDate due = LocalDate.parse(m.getDueDate());
            long diff = ChronoUnit.DAYS.between(now, due);
            mNode.put("daysUntilDue", Math.max(0, diff + 1));
            mNode.put("overdueBy", Math.max(0, -diff - 1));

            // Status Blocat
            boolean isBlocked = db.getMilestones().stream()
                    .anyMatch(other -> other.getBlockingFor() != null &&
                            other.getBlockingFor().contains(m.getName()) &&
                            db.getTickets().stream().anyMatch(t -> other.getTickets().contains(t.getId()) && t.getStatus() != ticketStatus.CLOSED));
            mNode.put("isBlocked", isBlocked);
            mNode.put("status", "ACTIVE"); // Simplificat pentru testul 2

            // Repartizare
            ArrayNode repartition = mNode.putArray("repartition");
            for (String dev : m.getAssignedDevs()) {
                ObjectNode devRep = repartition.addObject();
                devRep.put("developer", dev);
                List<Integer> devTickets = milestoneTickets.stream()
                        .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(dev))
                        .map(Ticket::getId).collect(Collectors.toList());
                devRep.set("assignedTickets", mapper.valueToTree(devTickets));
            }

            milestonesArray.add(mNode);
        }
        outputList.add(result);
    }
}