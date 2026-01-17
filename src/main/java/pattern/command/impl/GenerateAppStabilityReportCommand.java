package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.user.User;
import pattern.command.Command;
import pattern.strategy.CustomerImpactStrategy;
import pattern.strategy.TicketRiskStrategy;
import repository.Database;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GenerateAppStabilityReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GenerateAppStabilityReportCommand(final JsonNode commandNode,
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

        User user = db.findUserByUsername(username);
        if (user == null || user.getRole() != model.enums.Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "appStabilityReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: "
                    + "required role MANAGER; user role "
                    + (user != null ? user.getRole() : "null") + ".");
            outputs.add(error);
            return;
        }

        ObjectNode riskReport = new TicketRiskStrategy().calculate(mapper, db);
        ObjectNode impactReport = new CustomerImpactStrategy().calculate(mapper, db);

        int totalOpenTickets = riskReport.get("totalTickets").asInt();
        JsonNode risks = riskReport.get("riskByType");
        JsonNode impacts = impactReport.get("customerImpactByType");

        String stabilityStatus = determineStability(totalOpenTickets, risks, impacts);

        ObjectNode reportData = mapper.createObjectNode();
        reportData.put("totalOpenTickets", totalOpenTickets);
        reportData.set("openTicketsByType", riskReport.get("ticketsByType"));
        reportData.set("openTicketsByPriority", riskReport.get("ticketsByPriority"));
        reportData.set("riskByType", risks);
        reportData.set("impactByType", impacts);
        reportData.put("appStability", stabilityStatus);

        ObjectNode output = mapper.createObjectNode();
        output.put("command", "appStabilityReport");
        output.put("username", username);
        output.put("timestamp", timestamp);
        output.set("report", reportData);

        outputs.add(output);

        if ("STABLE".equals(stabilityStatus)) {
            db.closeApp();
        }
    }

    private String determineStability(final int totalTickets,
                                      final JsonNode risks, final JsonNode impacts) {
        if (totalTickets == 0) {
            return "STABLE";
        }

        boolean hasSignificantRisk = false;
        boolean allRisksNegligible = true;

        Iterator<Map.Entry<String, JsonNode>> riskFields = risks.fields();
        while (riskFields.hasNext()) {
            String riskLabel = riskFields.next().getValue().asText();
            if ("SIGNIFICANT".equals(riskLabel) || "MAJOR".equals(riskLabel)) {
                hasSignificantRisk = true;
            }
            if (!"NEGLIGIBLE".equals(riskLabel)) {
                allRisksNegligible = false;
            }
        }

        if (hasSignificantRisk) {
            return "UNSTABLE";
        }

        boolean allImpactsLow = true;
        Iterator<Map.Entry<String, JsonNode>> impactFields = impacts.fields();
        while (impactFields.hasNext()) {
            if (impactFields.next().getValue().asDouble() >= 50.0) {
                allImpactsLow = false;
                break;
            }
        }

        if (allRisksNegligible && allImpactsLow) {
            return "STABLE";
        }

        return "PARTIALLY STABLE";
    }
}
