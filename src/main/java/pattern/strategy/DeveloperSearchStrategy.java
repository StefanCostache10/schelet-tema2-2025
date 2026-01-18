package pattern.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.enums.Expertise;
import model.enums.Role;
import model.enums.Seniority;
import model.user.Developer;
import model.user.Manager;
import model.user.User;
import repository.Database;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DeveloperSearchStrategy implements SearchStrategy {
    @Override
    public List<ObjectNode> search(final JsonNode filters, final String requesterUsername,
                                   final ObjectMapper mapper, final Database db,
                                   final String timestamp) {
        User requester = db.findUserByUsername(requesterUsername);
        List<String> allowedUsernames = null;
        if (requester != null && requester.getRole() == Role.MANAGER) {
            allowedUsernames = ((Manager) requester).getSubordinates();
        }

        List<String> finalAllowedUsernames = allowedUsernames;
        List<Developer> developers = db.getUsers().stream()
                .filter(u -> u.getRole() == Role.DEVELOPER)
                .map(u -> (Developer) u)
                .filter(d -> finalAllowedUsernames == null
                        || finalAllowedUsernames.contains(d.getUsername()))
                .collect(Collectors.toList());

        if (filters.has("expertiseArea")) {
            String expStr = filters.get("expertiseArea").asText();
            developers = developers.stream()
                    .filter(d -> d.getExpertiseArea() == Expertise.valueOf(expStr))
                    .collect(Collectors.toList());
        }

        if (filters.has("seniority")) {
            String senStr = filters.get("seniority").asText();
            developers = developers.stream()
                    .filter(d -> d.getSeniority() == Seniority.valueOf(senStr))
                    .collect(Collectors.toList());
        }

        if (filters.has("performanceScoreAbove")) {
            double threshold = filters.get("performanceScoreAbove").asDouble();
            developers = developers.stream()
                    .filter(d -> d.getPerformanceScore() >= threshold)
                    .collect(Collectors.toList());
        }
        if (filters.has("performanceScoreBelow")) {
            double threshold = filters.get("performanceScoreBelow").asDouble();
            developers = developers.stream()
                    .filter(d -> d.getPerformanceScore() <= threshold)
                    .collect(Collectors.toList());
        }

        developers.sort(Comparator.comparing(Developer::getUsername));

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
