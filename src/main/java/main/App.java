package main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.user.User;
import pattern.command.Command;
import pattern.command.impl.ReportTicketCommand;
import pattern.command.impl.ViewTicketsCommand;
import repository.Database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App {
    private App() {
    }

    private static final String INPUT_USERS_FIELD = "input/database/users.json";
    private static final ObjectWriter WRITER = new ObjectMapper().writer().withDefaultPrettyPrinter();

    public static void run(final String inputPath, final String outputPath) {
        // 1. Resetăm baza de date la fiecare rulare de test
        Database db = Database.getInstance();
        db.reset();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Pentru date calendaristice

        // Lista unde vom stoca rezultatele (JSON Output)
        List<ObjectNode> outputs = new ArrayList<>();

        try {
            // 2. Încărcăm utilizatorii în baza de date
            File usersFile = new File(INPUT_USERS_FIELD);
            if (usersFile.exists()) {
                List<User> users = mapper.readValue(usersFile, new TypeReference<List<User>>() {});
                db.setUsers(users);
            } else {
                System.err.println("Users file not found: " + INPUT_USERS_FIELD);
            }

            // 3. Citim fișierul de comenzi
            File inputFile = new File(inputPath);
            JsonNode commandsArray = mapper.readTree(inputFile);

            // 4. Procesăm fiecare comandă
            if (commandsArray.isArray()) {
                for (JsonNode commandNode : commandsArray) {
                    String commandName = commandNode.get("command").asText();
                    Command command = null;

                    switch (commandName) {
                        case "reportTicket":
                            command = new ReportTicketCommand(commandNode, outputs, mapper);
                            break;
                        case "viewTickets":
                            command = new ViewTicketsCommand(commandNode, outputs, mapper);
                            break;

                        // Aici vei adăuga "createMilestone" când îl implementezi
                        // case "createMilestone":
                        //    command = new CreateMilestoneCommand(commandNode, outputs, mapper);
                        //    break;

                        default:
                            break;
                    }

                    if (command != null) {
                        command.execute();
                    }
                }
            }

            // 5. Scriem rezultatul final în fișierul de output
            File outputFile = new File(outputPath);
            // Creăm directoarele părinte dacă nu există (ex: "out/")
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            WRITER.writeValue(outputFile, outputs);

        } catch (IOException e) {
            System.out.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}