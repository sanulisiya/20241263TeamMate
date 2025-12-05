package cli;

import model.Participant;
import core.TeamFormationSystem;
import utility.LoggerService;
import java.util.*;

public class OrganizerCLI {
    //Logger for system
    private static final LoggerService logger = LoggerService.getInstance();
    //Constant for the Organizer PIN
    private static final String ORGANIZER_PIN = "1234";

    private final Scanner scanner;
    private String currentUploadedFilePath;
    private String teamsOutputPath;

    // *** Dependency Injection ***
    private final TeamFormationSystem system;

    private List<Participant> participants;
    private List<List<Participant>> teams;
    private List<Participant> remainingPool;
    private String updatedFilePath;

    // Constructor initializes fields and accepts the injected system
    public OrganizerCLI(Scanner scanner, String currentUploadedFilePath, String teamsOutputPath, TeamFormationSystem system) { //2.(SD-Organizer Login)
        this.scanner = scanner;
        this.currentUploadedFilePath = currentUploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
        this.participants = new ArrayList<>();
        this.teams = null;
        this.remainingPool = new ArrayList<>();
        this.updatedFilePath = currentUploadedFilePath;
        this.system = system; // Initializing the injected dependency
    }

    //Method to authenticate the organizer using the PIN
    public boolean authenticate() {//2.1.(SD-Organizer Login)
        System.out.print("\nEnter Organizer PIN: "); //2.2.(SD-Organizer Login)
        String pin = scanner.nextLine().trim();//2.3.(SD-Organizer Login)

        if (!pin.equals(ORGANIZER_PIN)) { //2.4.(SD-Organizer Login)
            logger.warn("Failed organizer PIN attempt");
            System.out.println(" Incorrect PIN. Returning to main menu...\n");//2.5.(SD-Organizer Login)
            return false;//2.6.(SD-Organizer Login)
        }

        System.out.println("\n PIN Verified  Access Granted."); //2.7.(SD-Organizer Login
        logger.info("Organizer PIN verified successfully");
        return true; //2.8.(SD-Organizer Login)
    }

    //Method to display the menu options for the organizer
    public void showMenu() {  //3.(SD-Organizer Login)
        boolean organizerRunning = true;

        //3.1(SD-Organizer Login)
        while (organizerRunning) {
            try {
                System.out.println("\n-------- ORGANIZER PANEL ---------");
                System.out.println("1. Upload CSV");//01.(SD- upload csv)
                System.out.println("2. View All Participants");//01.(SD- View all Participant)
                System.out.println("3. Formation of Teams"); //01.(SD-Team Formation)
                System.out.println("4. Save Formed Teams");//01.(SD-save Teams)
                System.out.println("5. Back to Main Menu");
                System.out.print("Select option: ");

                int choice = getIntInput();//3.2(SD-Organizer Login)

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

    // Loads a CSV file of participants using the system interface.
    private void handleUploadCSV() {//1.1.(SD- upload csv)
        System.out.print("\nEnter CSV File Path: ");//1.2.(SD- upload csv)
        String path = scanner.nextLine(); //1.3.(SD- upload csv)
        try {
            //  Use system.loadParticipants
            participants = system.loadParticipants(path);//2.(SD- upload csv)
            if (participants != null && !participants.isEmpty()) {
                updatedFilePath = path;
                logger.info("CSV uploaded successfully: " + path + " with " + participants.size() + " participants");//1.6.(SD- upload csv)
                System.out.println("\n CSV Uploaded Successfully! Total Participants: " + participants.size()); //1.7.(SD- upload csv)
                System.out.println("   This file will now be used for participant login verification.");

                System.out.println("\nSample of loaded participants:");
                for (int i = 0; i < Math.min(3, participants.size()); i++) {
                    System.out.println("  " + participants.get(i));
                }
            } else {
                logger.warn("CSV upload failed or empty: " + path); //1.5.(SD- upload csv)
                System.out.println("CSV Upload Failed. Check file path or file format!");
            }
        } catch (Exception e) {
            logger.error("Error uploading CSV: " + path, e);
            System.out.println("Error uploading CSV: " + e.getMessage());
        }
    }

    // Displays all participants loaded from the currently active CSV file.
    private void handleViewParticipants() {  //01.(SD- View all Participant)
        if (updatedFilePath == null && participants.isEmpty()) {
            System.out.println("No file uploaded. Please upload a CSV first."); //1.2.(SD- View all Participant)
            return; //1.3.(SD- View all Participant)
        }

        try {
            if (updatedFilePath != null) {
                //  Use system.loadParticipants
                participants = system.loadParticipants(updatedFilePath); //02.(SD- View all Participant)
            }
            logger.info("Viewing all participants from: " + updatedFilePath);
            System.out.println("\n------- PARTICIPANT LIST --------"); //1.4.(SD- View all Participant)
            System.out.println("\nTotal Participants: " + participants.size()); //1.5.(SD- View all Participant)
            System.out.println("Source File: " + (updatedFilePath != null ? updatedFilePath : "Default file")); //1.5.(SD- View all Participant)

            for (int i = 0; i < participants.size(); i++) {
                System.out.println((i + 1) + "\n. " + participants.get(i));  //1.7.(SD- View all Participant)
            }
        } catch (Exception e) {
            logger.error("Error loading participants for viewing", e);
            System.out.println("Error loading participants: " + e.getMessage());
        }
    }

    // Triggers team formation through TeamFormationHandler.
    //01. (Team Formation Sequance Digram)
    private void handleTeamFormation() { //1.1.(SD-Team Formation)
        if (updatedFilePath == null) {//1.2.(SD-Team Formation)
            System.out.println("No file uploaded. Upload CSV first.");//1.3.(SD-Team Formation)
            return;
        }
        // *** Inject the system instance into the handler ***
        TeamFormationHandler teamFormationHandler = new TeamFormationHandler(scanner, updatedFilePath, teamsOutputPath);
        TeamFormationResult result = teamFormationHandler.handleTeamFormation();

        if (result != null) {
            this.teams = result.getTeams();
            this.remainingPool = result.getRemainingPool();
            this.updatedFilePath = result.getUpdatedFilePath();
        }
    }

    // Saves currently formed teams into a CSV file using the system interface.

    private void handleSaveTeams() {   //1.1.(SD-save Teams)
        if (teams == null || teams.isEmpty()) {   //1.2.(SD-save Teams)
            System.out.println("Teams not formed yet. Please form teams first (Option 3)."); //1.4.(SD-save Teams)
            return; //1.5.(SD-save Teams)
        }

        System.out.println("\nEnter the FULL file path where you want to save the team CSV:"); //1.6.(SD-save Teams)
        System.out.println("(Example: C:\\Users\\DELL\\Desktop\\final_teams.csv)");
        System.out.print("Path: ");//1.7.(SD-save Teams)
        String userPath = scanner.nextLine().trim();

        if (userPath.isEmpty()) { //1.8.(SD-save Teams)
            System.out.println("Invalid path. Saving cancelled.");//1.9.(SD-save Teams)
            return; //1.10.(SD-save Teams)
        }

        if (!userPath.toLowerCase().endsWith(".csv")) {
            userPath = userPath + ".csv";
        }

        try {
            //  Use system.saveTeams
            system.saveTeams(teams, userPath);
            teamsOutputPath = userPath;//1.11.(SD-Save teams)
            logger.info("Teams saved successfully to: " + teamsOutputPath);
            System.out.println("\n Teams successfully saved!");//1.12.(SD-Save teams)
            System.out.println("\n Saved to: " + teamsOutputPath);//1.13.(SD-Save teams)
            System.out.println("   Total formatted teams saved: " + teams.size()); //1.14.(SD-Save teams)
            System.out.println("\n Participants can now view their assigned teams in the participant menu.");//1.15.(SD-Save teams)
        } catch (Exception e) {
            logger.error("Error saving teams to: " + userPath, e);
            System.out.println(" Error saving teams: " + e.getMessage());
        }
    }
    //3.4.(SD-Organizer Login)
    private int getIntInput() {  //3.4.(SD-Organizer Login)
        try {
            int input = scanner.nextInt();
            scanner.nextLine();
            return input;
        } catch (Exception e) {
            scanner.nextLine();
            return -1;
        }
    }
    //3.5.(SD-Organizer Login)
    public String getCurrentUploadedFilePath() {
        return updatedFilePath;
    }
    //3.6.(SD-Organizer Login)
    public String getTeamsOutputPath() {
        return teamsOutputPath;///1.16.(SD-Save teams)
    }
}