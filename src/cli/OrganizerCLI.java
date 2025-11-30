package cli;

import model.Participant;
import service.*;
import utility.LoggerService;
import java.util.*;


public class OrganizerCLI {
    //Logger for system
    private static final LoggerService logger = LoggerService.getInstance();
    private static final String ORGANIZER_PIN = "1234";

    private Scanner scanner;
    private String currentUploadedFilePath;
    private String teamsOutputPath;

    private List<Participant> participants;
    private List<List<Participant>> teams;
    private List<Participant> remainingPool;
    private String updatedFilePath;

    //Constructor initializes fields
    public OrganizerCLI(Scanner scanner, String currentUploadedFilePath, String teamsOutputPath) {
        this.scanner = scanner;
        this.currentUploadedFilePath = currentUploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
        this.participants = new ArrayList<>();
        this.teams = null;
        this.remainingPool = new ArrayList<>();
        this.updatedFilePath = currentUploadedFilePath;
    }

    public boolean authenticate() {
        System.out.print("\nEnter Organizer PIN: ");
        String pin = scanner.nextLine().trim();

        if (!pin.equals(ORGANIZER_PIN)) {
            logger.warn("Failed organizer PIN attempt");
            System.out.println(" Incorrect PIN. Returning to main menu...\n");
            return false;
        }

        //Authenticates organizer using a PIN

        System.out.println("\n PIN Verified  Access Granted.");
        logger.info("Organizer PIN verified successfully");
        return true;
    }
    //Main menu loop for organizer operations.

    public void showMenu() {
        boolean organizerRunning = true;

        while (organizerRunning) {
            try {
                System.out.println("\n-------- ORGANIZER PANEL ---------");
                System.out.println("1. Upload CSV");
                System.out.println("2. View All Participants");
                System.out.println("3. Formation of Teams");
                System.out.println("4. Save Formed Teams");
                System.out.println("5. Back to Main Menu");
                System.out.print("Select option: ");

                int choice = getIntInput();

                switch (choice) {
                    case 1:
                        handleUploadCSV();
                        break;
                    case 2:
                        handleViewParticipants();
                        break;
                    case 3:
                        handleTeamFormation();
                        break;
                    case 4:
                        handleSaveTeams();
                        break;

                    case 5:
                        organizerRunning = false;
                        logger.info("Organizer returning to main menu");
                        System.out.println("Returning to main menu...");
                        break;
                    default:
                        System.out.println("Invalid option. Try again!");
                        break;
                }
            } catch (Exception e) {
                logger.error("An error occurred in organizer panel", e);
                System.out.println("An error occurred in organizer panel: " + e.getMessage());
            }
        }
    }
    //Loads a CSV file of participants using FileHandler.
    private void handleUploadCSV() {
        System.out.print("\nEnter CSV File Path: ");
        String path = scanner.nextLine();
        try {
            participants = FileHandler.loadParticipantsSingleThread(path);
            if (participants != null && !participants.isEmpty()) {
                updatedFilePath = path;
                logger.info("CSV uploaded successfully: " + path + " with " + participants.size() + " participants");
                System.out.println("CSV Uploaded Successfully! Total Participants: " + participants.size());
                System.out.println("This file will now be used for participant login verification.");

                // Display sample of loaded participants
                System.out.println("\nSample of loaded participants:");
                for (int i = 0; i < Math.min(3, participants.size()); i++) {
                    System.out.println("  " + participants.get(i));
                }
            } else {
                logger.warn("CSV upload failed or empty: " + path);
                System.out.println("CSV Upload Failed. Check file path or file format!");
            }
        } catch (Exception e) {
            logger.error("Error uploading CSV: " + path, e);
            System.out.println("Error uploading CSV: " + e.getMessage());
        }
    }
//Displays all participants loaded from the currently active CSV file.

    private void handleViewParticipants() {
        if (updatedFilePath == null && participants.isEmpty()) {
            System.out.println("No file uploaded. Please upload a CSV first.");
            return;
        }

        try {
            if (updatedFilePath != null) {
                participants = FileHandler.loadParticipantsSingleThread(updatedFilePath);
            }
            logger.info("Viewing all participants from: " + updatedFilePath);
            System.out.println("\n----- PARTICIPANT LIST -----");
            System.out.println("Total Participants: " + participants.size());
            System.out.println("Source File: " + (updatedFilePath != null ? updatedFilePath : "Default file"));
            for (int i = 0; i < participants.size(); i++) {
                System.out.println((i + 1) + ". " + participants.get(i));
            }
        } catch (Exception e) {
            logger.error("Error loading participants for viewing", e);
            System.out.println("Error loading participants: " + e.getMessage());
        }
    }
//Triggers team formation through TeamFormationHandler.

    private void handleTeamFormation() {
        if (updatedFilePath == null) {
            System.out.println("No file uploaded. Upload CSV first.");
            return;
        }
// Delegate the logic to TeamFormationHandler
        TeamFormationHandler teamFormationHandler = new TeamFormationHandler(scanner, updatedFilePath, teamsOutputPath);
        TeamFormationResult result = teamFormationHandler.handleTeamFormation();

        if (result != null) {
            this.teams = result.getTeams();
            this.remainingPool = result.getRemainingPool();
            this.updatedFilePath = result.getUpdatedFilePath();
        }
    }
    //Saves currently formed teams into a CSV file chosen by the organizer.
    private void handleSaveTeams() {
        if (teams == null || teams.isEmpty()) {
            System.out.println("Teams not formed yet. Please form teams first (Option 3).");
            return;
        }

        System.out.println("\nEnter the FULL file path where you want to save the team CSV:");
        System.out.println("(Example: C:\\Users\\DELL\\Desktop\\final_teams.csv)");
        System.out.print("Path: ");
        String userPath = scanner.nextLine().trim();

        if (userPath.isEmpty()) {
            System.out.println("Invalid path. Saving cancelled.");
            return;
        }

        // Ensure file ends with .csv
        if (!userPath.toLowerCase().endsWith(".csv")) {
            userPath = userPath + ".csv";
        }

        try {
            TeamFileHandler.saveTeamsToCSV(teams, userPath);
            teamsOutputPath = userPath;
            logger.info("Teams saved successfully to: " + teamsOutputPath);
            System.out.println("\n Teams successfully saved!");
            System.out.println("Saved to: " + teamsOutputPath);
            System.out.println("Total formatted teams saved: " + teams.size());
            System.out.println("Participants can now view their assigned teams in the participant menu.");
        } catch (Exception e) {
            logger.error("Error saving teams to: " + userPath, e);
            System.out.println(" Error saving teams: " + e.getMessage());
        }
    }
    private int getIntInput() {
        try {
            int input = scanner.nextInt();
            scanner.nextLine();
            return input;
        } catch (Exception e) {
            scanner.nextLine();
            return -1;
        }
    }

    // Getters for main class to access updated state
    public String getCurrentUploadedFilePath() {
        return updatedFilePath;
    }

    public String getTeamsOutputPath() {
        return teamsOutputPath;
    }
}