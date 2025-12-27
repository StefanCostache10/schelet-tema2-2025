package pattern.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import model.ticket.*;
import model.enums.ticketType;

public class ticketFactory {

    /**
     * Metodă statică (Factory Method) care creează un tichet din nodul JSON.
     * Folosește ObjectMapper pentru a popula câmpurile automat.
     */
    public static Ticket createTicketFromCommand(JsonNode commandNode, ObjectMapper mapper) throws JsonProcessingException {
        // 1. Identificăm tipul tichetului
        if (!commandNode.has("type")) {
            throw new IllegalArgumentException("Ticket type missing in command");
        }

        String typeStr = commandNode.get("type").asText();
        ticketType type = ticketType.valueOf(typeStr);

        // 2. Instanțiem clasa corectă folosind Jackson
        Ticket ticket = null;

        switch (type) {
            case BUG:
                ticket = mapper.treeToValue(commandNode, Bug.class);
                break;
            case FEATURE_REQUEST:
                ticket = mapper.treeToValue(commandNode, featureRequest.class);
                break;
            case UI_FEEDBACK:
                ticket = mapper.treeToValue(commandNode, UIFeedback.class);
                break;
            default:
                throw new IllegalArgumentException("Unknown ticket type: " + type);
        }

        // 3. Setăm manual câmpuri care nu sunt direct în structura tichetului din comandă
        // De exemplu, 'username' din comandă devine 'reportedBy' în tichet
        if (commandNode.has("username")) {
            ticket.setReportedBy(commandNode.get("username").asText());
        }

        return ticket;
    }
}