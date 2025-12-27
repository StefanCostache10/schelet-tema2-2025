package pattern.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.ticket.Ticket;
import model.enums.Role;
import model.enums.ticketPriority;
import model.enums.ticketType;
import model.user.User;
import pattern.command.Command;
import pattern.factory.ticketFactory;
import repository.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportTicketCommand implements Command {
    private final JsonNode commandNode;
    private final List<ObjectNode> outputList;
    private final ObjectMapper mapper;
    private final Database db;

    public ReportTicketCommand(JsonNode commandNode, List<ObjectNode> outputList, ObjectMapper mapper) {
        this.commandNode = commandNode;
        this.outputList = outputList;
        this.mapper = mapper;
        this.db = Database.getInstance();
    }

    @Override
    public void execute() {
        String username = commandNode.get("username").asText();
        String timestamp = commandNode.get("timestamp").asText();

        // Setup App Start Date dacă nu e setat (la prima comandă)
        LocalDate currentDate = LocalDate.parse(timestamp);
        if (db.getAppStartDate() == null) {
            db.setAppStartDate(currentDate);
        }

        User user = db.findUserByUsername(username);

        // 1. Validare: Utilizator inexistent
        if (user == null) {
            addError(username, "The user " + username + " does not exist.", timestamp); // Mesaj fixat
            return;
        }

        // 2. Validare: Rol nepermis (Doar REPORTER)
        if (user.getRole() != Role.REPORTER) {
            addError(username, "The user " + username + " does not have permission to execute this command: required role REPORTER; user role " + user.getRole(), timestamp);
            return;
        }

        // 3. Validare: Perioada de Testare (NOU)
        // Perioada de testare durează 12 zile (zilele 0..11). Dacă diferența >= 12, am depășit.
        long daysSinceStart = ChronoUnit.DAYS.between(db.getAppStartDate(), currentDate);
        if (daysSinceStart >= 12) { // De ex: 1 Oct -> 13 Oct sunt 12 zile diferență, deci a 13-a zi.
            addError(username, "Tickets can only be reported during testing phases.", timestamp);
            return;
        }

        try {
            // Extragem parametrii tichetului
            JsonNode paramsNode = commandNode.has("params") ? commandNode.get("params") : commandNode;

            // 4. Creare Tichet
            Ticket ticket = ticketFactory.createTicketFromCommand(paramsNode, mapper);
            ticket.setTimestamp(timestamp);

            // Validare specială: Tichete Anonime
            boolean isAnonymous = ticket.getReportedBy() == null || ticket.getReportedBy().isEmpty();

            if (isAnonymous) {
                if (ticket.getType() != ticketType.BUG) {
                    addError(username, "Anonymous reports are only allowed for tickets of type BUG.", timestamp); // Mesaj fixat
                    return;
                }
                ticket.setBusinessPriority(ticketPriority.LOW);
            }

            // Salvăm
            db.addTicket(ticket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metoda corectată pentru a genera erori "FLAT" (fără "output": {})
    private void addError(String username, String errorMessage, String timestamp) {
        ObjectNode errorJson = mapper.createObjectNode();
        errorJson.put("command", "reportTicket");
        errorJson.put("username", username);
        errorJson.put("timestamp", timestamp);
        errorJson.put("error", errorMessage);
        outputList.add(errorJson);
    }
}