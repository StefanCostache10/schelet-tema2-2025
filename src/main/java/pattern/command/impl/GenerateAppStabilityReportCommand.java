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

public class GenerateAppStabilityReportCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputs;
    private final ObjectMapper mapper;
    private final Database db;

    public GenerateAppStabilityReportCommand(JsonNode commandNode, List<ObjectNode> outputs, ObjectMapper mapper) {
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
        // 1. Verificare Permisiuni (Manager)
        if (user == null || user.getRole() != model.enums.Role.MANAGER) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "appStabilityReport");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "The user does not have permission to execute this command: required role MANAGER; user role " + (user != null ? user.getRole() : "null") + ".");
            outputs.add(error);
            return;
        }

        // 2. Obținere date din strategii existente
        // TicketRiskStrategy calculează deja pe baza tichetelor OPEN și IN_PROGRESS, exact ce avem nevoie.
        ObjectNode riskReport = new TicketRiskStrategy().calculate(mapper, db);
        ObjectNode impactReport = new CustomerImpactStrategy().calculate(mapper, db);

        // 3. Extragere date pentru raportul final
        int totalOpenTickets = riskReport.get("totalTickets").asInt();
        JsonNode risks = riskReport.get("riskByType");
        JsonNode impacts = impactReport.get("customerImpactByType");

        // 4. Determinare Stabilitate
        String stabilityStatus = determineStability(totalOpenTickets, risks, impacts);

        // 5. Construire Output
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

        // 6. Oprire aplicație dacă este STABLE
        if ("STABLE".equals(stabilityStatus)) {
            db.closeApp();
        }
    }

    private String determineStability(int totalTickets, JsonNode risks, JsonNode impacts) {
        // Regula 1: Dacă nu există tichete OPEN/IN_PROGRESS -> STABLE
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

        // Regula 3: Dacă există cel puțin o categorie SIGNIFICANT (sau mai mare) -> UNSTABLE
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

        // Regula 2: Riscuri NEGLIGIBLE și Impact < 50 -> STABLE
        if (allRisksNegligible && allImpactsLow) {
            return "STABLE";
        }

        // Regula 4: Altfel -> PARTIALLY STABLE
        return "PARTIALLY STABLE";
    }
}