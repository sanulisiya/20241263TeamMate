import cli.MainMenuHandler;
import cli.OrganizerCLI;
import cli.ParticipantCLI;
import service.SurveyThreadManager;
import utility.LoggerService;

import java.io.File;
import java.util.Scanner;

public class MainCLI {
    private static String TEAMS_OUTPUT_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "formatted_teams.csv";
    private static String currentUploadedFilePath = null;
    private static final LoggerService logger = LoggerService.getInstance();

    public static void main(String[] args) {
        logger.info("Application started");

        Scanner scanner = new Scanner(System.in);
        MainMenuHandler mainMenu = new MainMenuHandler(scanner);

        while (true) {
            try {
                mainMenu.displayMainMenu();
                int loginChoice = mainMenu.getLoginChoice();

                switch (loginChoice) {
                    case 1:
                        handleParticipantFlow(scanner);
                        break;
                    case 2:
                        handleOrganizerFlow(scanner);
                        break;
                    case 3:
                        exitApplication(scanner);
                        break;
                    default:
                        System.out.println("Invalid Login Option!");
                        break;
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred in main loop", e);
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private static void handleParticipantFlow(Scanner scanner) {
        ParticipantCLI participantCLI = new ParticipantCLI(scanner, currentUploadedFilePath, TEAMS_OUTPUT_PATH);
        participantCLI.showMenu();
        // Update global state if participant added new file
        if (participantCLI.getCurrentUploadedFilePath() != null) {
            currentUploadedFilePath = participantCLI.getCurrentUploadedFilePath();
        }
    }


    private static void handleOrganizerFlow(Scanner scanner) {
        OrganizerCLI organizerCLI = new OrganizerCLI(scanner, currentUploadedFilePath, TEAMS_OUTPUT_PATH);
        if (organizerCLI.authenticate()) {
            organizerCLI.showMenu();
            // Update global state with any changes from organizer session
            currentUploadedFilePath = organizerCLI.getCurrentUploadedFilePath();
            TEAMS_OUTPUT_PATH = organizerCLI.getTeamsOutputPath();
        }
    }

    private static void exitApplication(Scanner scanner) {
        logger.info("Application exiting");
        System.out.println("\nExiting system... Goodbye!");
        scanner.close();
        System.exit(0);
    }
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SurveyThreadManager.shutdown();
        }));
    }
}