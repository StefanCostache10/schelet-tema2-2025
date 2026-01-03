package main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.user.User;
import pattern.command.Command;
import pattern.command.impl.*;
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
        Database db = Database.getInstance();
        db.reset();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        List<ObjectNode> outputs = new ArrayList<>();

        try {
            File usersFile = new File(INPUT_USERS_FIELD);
            if (usersFile.exists()) {
                List<User> users = mapper.readValue(usersFile, new TypeReference<List<User>>() {});
                db.setUsers(users);
            }

            File inputFile = new File(inputPath);
            JsonNode commandsArray = mapper.readTree(inputFile);

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
                        case "createMilestone":
                            command = new CreateMilestoneCommand(commandNode, outputs, mapper);
                            break;
                        case "viewMilestones":
                            command = new ViewMilestonesCommand(commandNode, outputs, mapper);
                            break;
                        case "assignTicket":
                            command = new AssignTicketCommand(commandNode, outputs, mapper);
                            break;
                        case "undoAssignTicket":
                            command = new UndoAssignTicketCommand(commandNode, outputs, mapper);
                            break;
                        case "addComment":
                            command = new AddCommentCommand(commandNode, outputs, mapper);
                            break;
                        case "undoAddComment":
                            command = new UndoAddCommentCommand(commandNode, outputs, mapper);
                            break;
                        case "viewAssignedTickets":
                            command = new ViewAssignedTicketsCommand(commandNode, outputs, mapper);
                            break;
                        case "lostInvestors":
                            db.closeApp();
                            break;
                        case "changeStatus":
                            command = new ChangeStatusCommand(commandNode, outputs, mapper);
                            break;
                        // --- Comenzi NOI adăugate ---
                        case "undoChangeStatus":
                            command = new UndoChangeStatusCommand(commandNode, outputs, mapper);
                            break;
                        case "viewTicketHistory":
                            command = new ViewTicketHistoryCommand(commandNode, outputs, mapper);
                            break;
                        case "search":
                            command = new SearchCommand(commandNode, outputs, mapper);
                            break;
                        // ----------------------------
                        default:
                            break;
                    }

                    if (command != null) {
                        command.execute();
                    }
                }
            }

            File outputFile = new File(outputPath);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            WRITER.writeValue(outputFile, outputs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}