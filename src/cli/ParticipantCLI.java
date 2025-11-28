package cli;

import model.Participant;
import service.FileHandler;
import service.ParticipantCreator;
import utility.LoggerService;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ParticipantCLI {
    private static final LoggerService logger = LoggerService.getInstance();
    private Scanner scanner;
    private String currentUploadedFilePath;
    private String teamsOutputPath;
    private String updatedFilePath;

    public ParticipantCLI(Scanner scanner, String currentUploadedFilePath, String teamsOutputPath) {
        this.scanner = scanner;
        this.currentUploadedFilePath = currentUploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
    }

    public void showMenu() {
        logger.info("Participant menu accessed");

        System.out.println("\n--- PARTICIPANT MENU ---");
        System.out.println("1. Add New Participant");
        System.out.println("2. Login as Existing Participant");
        System.out.print("Select option: ");

        int participantChoice = getIntInput();

        switch (participantChoice) {
            case 1:
                handleAddNewParticipant();
                break;
            case 2:
                handleExistingParticipantLogin();
                break;
            default:
                logger.warn("Invalid participant option selected: " + participantChoice);
                System.out.println("Invalid participant option.");
                break;
        }
    }

    private void handleAddNewParticipant() {
        logger.info("Add new participant option selected");

        try {
            System.out.println("\nEnter the folder path where you want to save participant_data.csv");
            System.out.println("Example: C:\\Users\\DELL\\Desktop");
            System.out.print("Path: ");

            String folderPath = scanner.nextLine().trim();

            if (folderPath.isEmpty()) {
                logger.warn("Empty folder path provided for new participant");
                System.out.println("Invalid folder. Returning to menu...\n");
                return;
            }

            // Ensure folder exists
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.warn("Invalid folder path provided: " + folderPath);
                System.out.println("Folder not found. Returning to menu...\n");
                return;
            }

            // Create new CSV inside that folder
            String newCSVPath = folderPath + File.separator + "participant_data.csv";

            File csvFile = new File(newCSVPath);
            if (!csvFile.exists()) {
                FileWriter writer = new FileWriter(csvFile);
                writer.write("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType,TeamNumber\n");
                writer.close();
                logger.info("New CSV file created: " + newCSVPath);
                System.out.println("\nCreated new CSV file: " + newCSVPath);
            } else {
                logger.info("Existing CSV file accessed: " + newCSVPath);
                System.out.println("\nCSV file already exists. Adding participant to it.");
            }

            // Add the participant
            logger.info("Starting participant creation process");
            ParticipantCreator.createNewParticipant(newCSVPath);

            logger.info("Participant added successfully");
            System.out.println("\nParticipant added successfully!");
            System.out.println("Saved to: " + newCSVPath);

            // Update the file path for organizer use
            updatedFilePath = newCSVPath;

        } catch (Exception e) {
            logger.error("Failed to add new participant", e);
            System.out.println("Failed to add participant: " + e.getMessage());
        }

        System.out.println("Returning to main menu...\n");
    }

    private void handleExistingParticipantLogin() {
        logger.info("Existing participant login option selected");

        // Check if organizer has uploaded any file
        if (currentUploadedFilePath == null) {
            System.out.println("\n Organizer didn't upload any file yet.");
            System.out.println("Please ask the organizer to upload a CSV file first.");
            System.out.println("Returning to main menu...\n");
            return;
        }

        System.out.print("Enter Participant ID: ");
        String participantId = scanner.nextLine().trim();

        logger.info("Participant login attempt: " + participantId);

        try {
            // Use the current uploaded file path from organizer for login verification
            String loginFilePath = currentUploadedFilePath;

            logger.info("Loading participants for login verification from: " + loginFilePath);
            List<Participant> participants = FileHandler.loadParticipantsSingleThread(loginFilePath);

            Participant found = participants.stream()
                    .filter(p -> p.getId().equalsIgnoreCase(participantId))
                    .findFirst()
                    .orElse(null);

            if (found != null) {
                handleSuccessfulLogin(found, participantId);
            } else {
                handleFailedLogin(participantId);
            }
        } catch (Exception e) {
            logger.error("Error loading participants for login", e);
            System.out.println("Error loading participants: " + e.getMessage());
        }
    }

    private void handleSuccessfulLogin(Participant participant, String participantId) {
        logger.info("Participant login successful: " + participantId);

        System.out.println("\nParticipant Found!");
        System.out.println(participant);

        System.out.print("\nDo you want to view your assigned team? (yes/no): ");
        String yn = scanner.nextLine().trim().toLowerCase();

        if (yn.equals("yes") || yn.equals("y")) {
            logger.info("Participant requested team view: " + participantId);
            showTeamAssignment(participantId);
        } else {
            logger.info("Participant declined team view: " + participantId);
        }

        System.out.println("\nReturning to main menu...\n");
    }

    private void handleFailedLogin(String participantId) {
        logger.warn("Participant not found: " + participantId);
        System.out.println("\nParticipant not found in current dataset.");
        System.out.println("Please check your ID or contact organizer.");
        System.out.println("Returning to main menu...");
    }

    private void showTeamAssignment(String participantId) {
        try {
            // Check if team file exists using the updated TEAMS_OUTPUT_PATH
            File teamFile = new File(teamsOutputPath);
            if (!teamFile.exists()) {
                logger.warn("Team file not found for participant: " + participantId);
                System.out.println("\nNo teams have been formed yet. Please check later!");
                return;
            }

            // Load participants from team file using the correct method
            logger.info("Loading team assignments for participant from: " + teamsOutputPath);
            List<Participant> teamParticipants = FileHandler.loadTeamsFromOutput(teamsOutputPath);

            if (teamParticipants.isEmpty()) {
                logger.warn("No team participants found in file: " + teamsOutputPath);
                System.out.println("\nNo teams found in the team file.");
                return;
            }

            // Find the participant in team file and get their team number
            Participant teamParticipant = teamParticipants.stream()
                    .filter(p -> p.getId().equalsIgnoreCase(participantId))
                    .findFirst()
                    .orElse(null);

            if (teamParticipant == null || teamParticipant.getTeamNumber() == null || teamParticipant.getTeamNumber().isEmpty()) {
                logger.warn("Participant not assigned to any team: " + participantId);
                System.out.println("\nYou are not assigned to any team yet.");
                return;
            }

            String teamNumber = teamParticipant.getTeamNumber();

            // Find all participants in the same team
            List<Participant> teamMembers = teamParticipants.stream()
                    .filter(p -> teamNumber.equals(p.getTeamNumber()))
                    .collect(Collectors.toList());

            logger.info("Displaying team " + teamNumber + " for participant: " + participantId);

            System.out.println("\n You are in TEAM " + teamNumber);
            System.out.println("---- Team Members ----");
            for (Participant member : teamMembers) {
                System.out.println("  " + member.getName() + " (" + member.getId() + ")");
                System.out.println("    Game: " + member.getPreferredGame() +
                        " | Skill: " + member.getSkillLevel() +
                        " | Role: " + member.getPreferredRole() +
                        " | Personality: " + member.getPersonalityType());
            }

            // Display team statistics
            displayTeamStats(teamMembers);

        } catch (Exception e) {
            logger.error("Error loading team assignments for participant: " + participantId, e);
            System.out.println("Error loading teams: " + e.getMessage());
            System.out.println("Please make sure teams have been formed and saved by the organizer.");
        }
    }

    private void displayTeamStats(List<Participant> team) {
        if (team == null || team.isEmpty()) return;

        // Calculate team statistics
        double avgSkill = team.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0.0);

        Map<String, Long> gameCount = team.stream()
                .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));

        Map<String, Long> roleCount = team.stream()
                .collect(Collectors.groupingBy(p -> p.getPreferredRole().name(), Collectors.counting()));

        Map<String, Long> personalityCount = team.stream()
                .collect(Collectors.groupingBy(p -> p.getPersonalityType().name(), Collectors.counting()));

        System.out.println("\n Team Statistics:");
        System.out.printf("   Average Skill Level: %.2f/10\n", avgSkill);
        System.out.println("   Game Distribution: " + formatMap(gameCount));
        System.out.println("   Role Distribution: " + formatMap(roleCount));
        System.out.println("   Personality Distribution: " + formatMap(personalityCount));
        System.out.println("   Team Size: " + team.size());

        // Check for team balance
        if (personalityCount.containsKey("LEADER")) {
            System.out.println("  Has a Leader");
        } else {
            System.out.println(" No Leader in team");
        }

        if (personalityCount.containsKey("THINKER") && personalityCount.get("THINKER") >= 1) {
            System.out.println("  Has Thinker(s)");
        } else {
            System.out.println(" No Thinker in team");
        }
    }

    private String formatMap(Map<String, Long> map) {
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
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

    public String getCurrentUploadedFilePath() {
        return updatedFilePath;
    }
}