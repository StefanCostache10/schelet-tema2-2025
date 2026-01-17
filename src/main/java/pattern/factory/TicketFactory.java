package pattern.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.enums.TicketType;
import model.ticket.Bug;
import model.ticket.FeatureRequest;
import model.ticket.Ticket;
import model.ticket.UIFeedback;

public final class TicketFactory {

    private TicketFactory() {
    }

    /**
     * Metodă statică (Factory Method) care creează un tichet din nodul JSON.
     * Folosește ObjectMapper pentru a popula câmpurile automat.
     */
    public static Ticket createTicketFromCommand(final JsonNode commandNode,
                                                 final ObjectMapper mapper)
            throws JsonProcessingException {
        if (!commandNode.has("type")) {
            throw new IllegalArgumentException("Ticket type missing in command");
        }

        String typeStr = commandNode.get("type").asText();
        TicketType type = TicketType.valueOf(typeStr);

        Ticket ticket = null;

        switch (type) {
            case BUG:
                ticket = mapper.treeToValue(commandNode, Bug.class);
                break;
            case FEATURE_REQUEST:
                ticket = mapper.treeToValue(commandNode, FeatureRequest.class);
                break;
            case UI_FEEDBACK:
                ticket = mapper.treeToValue(commandNode, UIFeedback.class);
                break;
            default:
                throw new IllegalArgumentException("Unknown ticket type: " + type);
        }


        return ticket;
    }
}
