package pattern.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import repository.Database;
import java.util.List;

public interface SearchStrategy {
    List<ObjectNode> search(JsonNode filters, String requesterUsername, ObjectMapper mapper, Database db, String timestamp);
}