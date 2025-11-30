package cli;

import model.Participant;
import service.FileHandler;
import service.ParticipantCreator;
import service.CSVMerger;
import utility.LoggerService;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ParticipantCLI {
    // Logger instance
    private static final LoggerService logger = LoggerService.getInstance();
    private Scanner scanner;
    //Path to organizer-uploaded CSV
    private String currentUploadedFilePath;
    private String teamsOutputPath;
    private String updatedFilePath;

    public ParticipantCLI(Scanner scanner, String currentUploadedFilePath, String teamsOutputPath) {
        this.scanner = scanner;
        this.currentUploadedFilePath = currentUploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
    }
    //Displays the participant menu and selection rutes

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

    //Handles creation of a new participant and stores them in merge pool
    private void handleAddNewParticipant() {
        logger.info("Add new participant option selected");

        try {
            System.out.println("\n=== REGISTER NEW PARTICIPANT ===");

            // Create participant using ParticipantCreator
            logger.info("Starting participant creation process");
            Participant newParticipant = ParticipantCreator.createNewParticipant();

            if (newParticipant != null) {
                // Add to merge pool instead of saving directly to CSV
                CSVMerger.addNewParticipant(newParticipant);

                logger.info("Participant added to merge pool: " + newParticipant.getId());
                System.out.println("\n Participant registered successfully!");
                System.out.println(" Note: This participant is now in the merge pool.");
                System.out.println("Organizer will merge them during team formation.");
                System.out.println(" Email: " + newParticipant.getEmail());
                System.out.println(" Temporary ID: " + newParticipant.getId());

                // Show current queue status
                int waitingCount = CSVMerger.getNewParticipantsCount();
                System.out.println("\n Merge Queue: " + waitingCount + " participants waiting to be merged");

                if (waitingCount > 1) {
                    System.out.println(" The organizer will merge all waiting participants during next team formation.");
                }
            } else {
                System.out.println(" Participant registration failed. Please try again.");
            }

        } catch (Exception e) {
            logger.error("Failed to add new participant", e);
            System.out.println(" Failed to register participant: " + e.getMessage());
        }

        System.out.println("Returning to main menu...\n");
    }

    // Shows current merge queue status
    private void handleViewMergeQueue() {
        int waitingCount = CSVMerger.getNewParticipantsCount();

        System.out.println("\n=== MERGE QUEUE STATUS ===");
        System.out.println("Participants waiting to be merged: " + waitingCount);

        if (waitingCount > 0) {
            System.out.println("\n Waiting Participants:");
            List<Participant> waitingParticipants = CSVMerger.getNewParticipants();
            for (int i = 0; i < waitingParticipants.size(); i++) {
                Participant p = waitingParticipants.get(i);
                System.out.println((i + 1) + ". " + p.getName() + " (" + p.getEmail() + ") - " + p.getPreferredGame());
            }
            System.out.println("\n These participants will be merged when the organizer forms teams.");
        } else {
            System.out.println("No participants waiting in the merge queue.");
        }
        System.out.println("\nReturning to menu...");
    }

    //Handles login verifications for exsiting participants
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
            System.out.println(" Error loading participants: " + e.getMessage());
        }
    }

    private void handleSuccessfulLogin(Participant participant, String participantId) {
        logger.info("Participant login successful: " + participantId);

        System.out.println("\n Participant Found!");
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

            System.out.println("\nYou are in TEAM " + teamNumber);
            System.out.println("------ Team Members ------");
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
    //Displays team statistics such as skill average, game distribution

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
            System.out.println(" Has a Leader");
        } else {
            System.out.println("  No Leader in team");
        }

        if (personalityCount.containsKey("THINKER") && personalityCount.get("THINKER") >= 1) {
            System.out.println(" Has Thinker(s)");
        } else {
            System.out.println(" No Thinker in team");
        }
    }
    //Utility function to convert key/value maps into printable strings.
    private String formatMap(Map<String, Long> map) {
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }
//Reads integer input safely.

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