package pattern.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.Expertise;
import model.enums.Role;
import model.enums.Seniority;
import model.user.Developer;
import model.user.User;
import pattern.strategy.SearchStrategy;
import repository.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeveloperSearchStrategy implements SearchStrategy {
    @Override
    public List<ObjectNode> search(JsonNode filters, String requesterUsername, ObjectMapper mapper, Database db, String timestamp) {
        List<Developer> developers = db.getUsers().stream()
                .filter(u -> u.getRole() == Role.DEVELOPER)
                .map(u -> (Developer) u)
                .collect(Collectors.toList());

        // Filtrare după expertiză
        if (filters.has("expertiseArea")) {
            String expStr = filters.get("expertiseArea").asText();
            developers = developers.stream()
                    .filter(d -> d.getExpertiseArea() == Expertise.valueOf(expStr))
                    .collect(Collectors.toList());
        }

        // Filtrare după senioritate
        if (filters.has("seniority")) {
            String senStr = filters.get("seniority").asText();
            developers = developers.stream()
                    .filter(d -> d.getSeniority() == Seniority.valueOf(senStr))
                    .collect(Collectors.toList());
        }

        // Mappare rezultate
        List<ObjectNode> results = new ArrayList<>();
        for (Developer dev : developers) {
            ObjectNode node = mapper.createObjectNode();
            node.put("username", dev.getUsername());
            node.put("expertiseArea", dev.getExpertiseArea().toString());
            node.put("seniority", dev.getSeniority().toString());
            node.put("performanceScore", dev.getPerformanceScore());
            node.put("hireDate", dev.getHireDate());
            results.add(node);
        }
        return results;
    }
}