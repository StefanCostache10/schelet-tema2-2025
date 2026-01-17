package main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.user.User;
import pattern.command.CreateMilestoneCommand;
import pattern.command.Command;
import pattern.command.impl.AddCommentCommand;
import pattern.command.impl.AssignTicketCommand;
import pattern.command.impl.ChangeStatusCommand;
import pattern.command.impl.GenerateAppStabilityReportCommand;
import pattern.command.impl.GenerateCustomerImpactReportCommand;
import pattern.command.impl.GeneratePerformanceReportCommand;
import pattern.command.impl.GenerateResolutionEfficiencyReportCommand;
import pattern.command.impl.GenerateTicketRiskReportCommand;
import pattern.command.impl.ReportTicketCommand;
import pattern.command.impl.SearchCommand;
import pattern.command.impl.StartTestingPhaseCommand;
import pattern.command.impl.UndoAddCommentCommand;
import pattern.command.impl.UndoAssignTicketCommand;
import pattern.command.impl.UndoChangeStatusCommand;
import pattern.command.impl.ViewAssignedTicketsCommand;
import pattern.command.impl.ViewMilestonesCommand;
import pattern.command.impl.ViewNotificationsCommand;
import pattern.command.impl.ViewTicketHistoryCommand;
import pattern.command.impl.ViewTicketsCommand;
import repository.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application entry point. Processes input commands and produces output.
 */
public final class App {
    private App() {
    }

    private static final String INPUT_USERS_FIELD = "input/database/users.json";
    private static final ObjectWriter WRITER = new ObjectMapper()
            .writer()
            .withDefaultPrettyPrinter();

    /**
     * Execute the application using the provided input and output paths.
     *
     * @param inputPath  path to the input JSON file with commands
     * @param outputPath path to the output JSON file to write results
     */
    public static void run(final String inputPath, final String outputPath) {
        Database db = Database.getInstance();
        db.reset();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        List<ObjectNode> outputs = new ArrayList<>();

        try {
            loadUsers(mapper, db);

            processCommands(mapper, db, inputPath, outputs);

            File outputFile = new File(outputPath);
            if (outputFile.getParentFile() != null) {
                Files.createDirectories(outputFile.getParentFile().toPath());
            }
            WRITER.writeValue(outputFile, outputs);

        } catch (IOException e) {
            System.err.println("I/O error in App.run: " + e.getMessage());
        }
    }

    private static void loadUsers(final ObjectMapper mapper, final Database db)
            throws IOException {
        File usersFile = new File(INPUT_USERS_FIELD);
        if (usersFile.exists()) {
            List<User> users = mapper.readValue(
                    usersFile,
                    new TypeReference<>() { }
            );
            db.setUsers(users);
        }
    }

    private static void processCommands(
            final ObjectMapper mapper,
            final Database db,
            final String inputPath,
            final List<ObjectNode> outputs
    ) throws IOException {
        File inputFile = new File(inputPath);
        JsonNode commandsArray = mapper.readTree(inputFile);

        if (!commandsArray.isArray()) {
            return;
        }

        for (JsonNode commandNode : commandsArray) {
            String commandName = commandNode.get("command").asText();
            String timestamp = commandNode.get("timestamp").asText();


            Database.getInstance().updateCurrentDate(timestamp);

            Command command = buildCommand(
                    commandName,
                    commandNode,
                    outputs,
                    mapper,
                    db
            );

            if (command != null) {
                command.execute();
            }

            if (db.isAppClosed()) {
                break;
            }
        }
    }

    private static Command buildCommand(
            final String commandName,
            final JsonNode commandNode,
            final List<ObjectNode> outputs,
            final ObjectMapper mapper,
            final Database db
    ) {
        return switch (commandName) {
            case "reportTicket" ->
                    new ReportTicketCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "viewTickets" ->
                    new ViewTicketsCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "createMilestone" ->
                    new CreateMilestoneCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "viewMilestones" ->
                    new ViewMilestonesCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "assignTicket" ->
                    new AssignTicketCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "undoAssignTicket" ->
                    new UndoAssignTicketCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "addComment" ->
                    new AddCommentCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "undoAddComment" ->
                    new UndoAddCommentCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "viewAssignedTickets" ->
                    new ViewAssignedTicketsCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "lostInvestors" -> {
                db.closeApp();
                yield null;
            }
            case "changeStatus" ->
                    new ChangeStatusCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "undoChangeStatus" ->
                    new UndoChangeStatusCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "viewTicketHistory" ->
                    new ViewTicketHistoryCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "search" ->
                    new SearchCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "viewNotifications" ->
                    new ViewNotificationsCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "generateCustomerImpactReport" ->
                    new GenerateCustomerImpactReportCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "generateTicketRiskReport" ->
                    new GenerateTicketRiskReportCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "generateResolutionEfficiencyReport" ->
                    new GenerateResolutionEfficiencyReportCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "appStabilityReport" ->
                    new GenerateAppStabilityReportCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "generatePerformanceReport" ->
                    new GeneratePerformanceReportCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            case "startTestingPhase" ->
                    new StartTestingPhaseCommand(
                            commandNode,
                            outputs,
                            mapper
                    );
            default -> null;
        };
    }
}
