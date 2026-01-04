package pattern.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import repository.Database;

public interface MetricStrategy {
    ObjectNode calculate(ObjectMapper mapper, Database db);
}