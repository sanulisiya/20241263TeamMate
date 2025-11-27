import model.Participant;
import service.*;
import utility.LoggerService;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class MainCLI {

    private static String TEAMS_OUTPUT_PATH = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";
    private static final String ORGANIZER_PIN = "1234";

    // Store the current uploaded file path for participant login checks
    private static String currentUploadedFilePath = null;

    // Logger instance
    private static final LoggerService logger = LoggerService.getInstance();

    public static void main(String[] args) {

        logger.info("Application started");

        Scanner sc = new Scanner(System.in);
        List<Participant> participants = new ArrayList<>();
        List<List<Participant>> teams = null;
        List<Participant> remainingPool = new ArrayList<>();

        while (true) {

            try {
                System.out.println("\n==== TEAMMATE COMMUNITY SYSTEM ====");
                System.out.println("Login as:");
                System.out.println("1. Participant");
                System.out.println("2. Organizer");
                System.out.println("3. Exit");
                System.out.print("Select option: ");

                int loginChoice = 0;
                try {
                    loginChoice = sc.nextInt();
                    sc.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a number.");
                    sc.nextLine();
                    continue;
                }

                // ============================= PARTICIPANT FLOW =============================
                if (loginChoice == 1) {
                    logger.info("Participant menu accessed");

                    System.out.println("\n--- PARTICIPANT MENU ---");
                    System.out.println("1. Add New Participant");
                    System.out.println("2. Login as Existing Participant");
                    System.out.print("Select option: ");

                    int participantChoice = 0;
                    try {
                        participantChoice = sc.nextInt();
                        sc.nextLine();
                    } catch (InputMismatchException e) {
                        logger.warn("Invalid input in participant menu - expected number");
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine();
                        continue;
                    }

                    // ---- ADD NEW PARTICIPANT ----
                    if (participantChoice == 1) {
                        logger.info("Add new participant option selected");

                        try {
                            System.out.println("\nEnter the folder path where you want to save participant_data.csv");
                            System.out.println("Example: C:\\Users\\DELL\\Desktop");
                            System.out.print("Path: ");

                            String folderPath = sc.nextLine().trim();

                            if (folderPath.isEmpty()) {
                                logger.warn("Empty folder path provided for new participant");
                                System.out.println("Invalid folder. Returning to menu...\n");
                                continue;
                            }

                            // Ensure folder exists
                            File folder = new File(folderPath);
                            if (!folder.exists() || !folder.isDirectory()) {
                                logger.warn("Invalid folder path provided: " + folderPath);
                                System.out.println("Folder not found. Returning to menu...\n");
                                continue;
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

                        } catch (Exception e) {
                            logger.error("Failed to add new participant", e);
                            System.out.println("Failed to add participant: " + e.getMessage());
                        }

                        System.out.println("Returning to main menu...\n");
                        continue;
                    }

                    // ---- EXISTING PARTICIPANT LOGIN ----
                    else if (participantChoice == 2) {
                        logger.info("Existing participant login option selected");

                        // Check if organizer has uploaded any file
                        if (currentUploadedFilePath == null) {
                            System.out.println("\n Organizer didn't upload any file yet.");
                            System.out.println("Please ask the organizer to upload a CSV file first.");
                            System.out.println("Returning to main menu...\n");
                            continue;
                        }

                        System.out.print("Enter Participant ID: ");
                        String participantId = sc.nextLine().trim();

                        logger.info("Participant login attempt: " + participantId);

                        try {
                            // Use the current uploaded file path from organizer for login verification
                            String loginFilePath = currentUploadedFilePath;

                            logger.info("Loading participants for login verification from: " + loginFilePath);
                            participants = FileHandler.loadParticipantsSingleThread(loginFilePath);

                        } catch (Exception e) {
                            logger.error("Error loading participants for login", e);
                            System.out.println("Error loading participants: " + e.getMessage());
                            continue;
                        }

                        Participant found = participants.stream()
                                .filter(p -> p.getId().equalsIgnoreCase(participantId))
                                .findFirst()
                                .orElse(null);

                        if (found != null) {
                            logger.info("Participant login successful: " + participantId);

                            System.out.println("\nParticipant Found!");
                            System.out.println(found);

                            System.out.print("\nDo you want to view your assigned team? (yes/no): ");
                            String yn = sc.nextLine().trim().toLowerCase();

                            if (yn.equals("yes") || yn.equals("y")) {
                                logger.info("Participant requested team view: " + participantId);

                                try {
                                    // Check if team file exists using the updated TEAMS_OUTPUT_PATH
                                    File teamFile = new File(TEAMS_OUTPUT_PATH);
                                    if (!teamFile.exists()) {
                                        logger.warn("Team file not found for participant: " + participantId);
                                        System.out.println("\nNo teams have been formed yet. Please check later!");
                                        System.out.println("\nReturning to main menu...\n");
                                        continue;
                                    }

                                    // Load participants from team file using the correct method
                                    logger.info("Loading team assignments for participant from: " + TEAMS_OUTPUT_PATH);
                                    List<Participant> teamParticipants = FileHandler.loadTeamsFromOutput(TEAMS_OUTPUT_PATH);

                                    if (teamParticipants.isEmpty()) {
                                        logger.warn("No team participants found in file: " + TEAMS_OUTPUT_PATH);
                                        System.out.println("\nNo teams found in the team file.");
                                        System.out.println("\nReturning to main menu...\n");
                                        continue;
                                    }

                                    // Find the participant in team file and get their team number
                                    Participant teamParticipant = teamParticipants.stream()
                                            .filter(p -> p.getId().equalsIgnoreCase(participantId))
                                            .findFirst()
                                            .orElse(null);

                                    if (teamParticipant == null || teamParticipant.getTeamNumber() == null || teamParticipant.getTeamNumber().isEmpty()) {
                                        logger.warn("Participant not assigned to any team: " + participantId);
                                        System.out.println("\nYou are not assigned to any team yet.");
                                        System.out.println("\nReturning to main menu...\n");
                                        continue;
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
                            } else {
                                logger.info("Participant declined team view: " + participantId);
                            }

                            System.out.println("\nReturning to main menu...\n");
                            continue;

                        } else {
                            logger.warn("Participant not found: " + participantId);
                            System.out.println("\nParticipant not found in current dataset.");
                            System.out.println("Please check your ID or contact organizer.");
                            System.out.println("Returning to main menu...");
                            continue;
                        }
                    } else {
                        logger.warn("Invalid participant option selected: " + participantChoice);
                        System.out.println("Invalid participant option.");
                        continue;
                    }
                }

                // ============================= ORGANIZER FLOW =============================
                else if (loginChoice == 2) {

                    // Ask for PIN before continuing
                    System.out.print("\nEnter Organizer PIN: ");
                    String pin = sc.nextLine().trim();

                    if (!pin.equals(ORGANIZER_PIN)) {
                        logger.warn("Failed organizer PIN attempt");
                        System.out.println(" Incorrect PIN. Returning to main menu...\n");
                        continue;
                    }

                    System.out.println("\n PIN Verified! Access Granted.");
                    logger.info("Organizer PIN verified successfully");

                    boolean organizerRunning = true;
                    String uploadedFilePath = null;

                    while (organizerRunning) {
                        try {
                            System.out.println("\n------ ORGANIZER PANEL -------");
                            System.out.println("1. Upload CSV");
                            System.out.println("2. View All Participants");
                            System.out.println("3. Formation of Teams");
                            System.out.println("4. Save Formed Teams");
                            System.out.println("5. View Team Assignments");
                            System.out.println("6. Back to Main Menu");
                            System.out.print("Select option: ");

                            int choice = 0;
                            try {
                                choice = sc.nextInt();
                                sc.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Invalid input. Please enter a number.");
                                sc.nextLine();
                                continue;
                            }

                            switch (choice) {
                                case 1:
                                    System.out.print("\nEnter CSV File Path: ");
                                    String path = sc.nextLine();
                                    try {
                                        participants = FileHandler.loadParticipantsSingleThread(path);
                                        if (participants != null && !participants.isEmpty()) {
                                            uploadedFilePath = path;
                                            currentUploadedFilePath = path; // Store for participant login
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
                                    break;

                                case 2:
                                    if (uploadedFilePath == null && participants.isEmpty()) {
                                        System.out.println("No file uploaded. Please upload a CSV first.");
                                    } else {
                                        try {
                                            if (uploadedFilePath != null) {
                                                participants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                            }
                                            logger.info("Viewing all participants from: " + uploadedFilePath);
                                            System.out.println("\n--- PARTICIPANT LIST ---");
                                            System.out.println("Total Participants: " + participants.size());
                                            System.out.println("Source File: " + (uploadedFilePath != null ? uploadedFilePath : "Default file"));
                                            for (int i = 0; i < participants.size(); i++) {
                                                System.out.println((i + 1) + ". " + participants.get(i));
                                            }
                                        } catch (Exception e) {
                                            logger.error("Error loading participants for viewing", e);
                                            System.out.println("Error loading participants: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 3:
                                    if (uploadedFilePath == null) {
                                        System.out.println("No file uploaded. Upload CSV first.");
                                        break;
                                    }

                                    // AUTO-MERGE FUNCTIONALITY
                                    System.out.print("\nDo you want to merge with participant-added data file? (yes/no): ");
                                    String mergeChoice = sc.nextLine().trim().toLowerCase();

                                    List<Participant> workingParticipants = new ArrayList<>();

                                    if (mergeChoice.equals("yes") || mergeChoice.equals("y")) {
                                        System.out.print("Enter path to participant-added CSV file: ");
                                        String participantFile = sc.nextLine().trim();

                                        if (!participantFile.isEmpty()) {
                                            try {
                                                // Check if participant file exists
                                                File partFile = new File(participantFile);
                                                if (!partFile.exists()) {
                                                    logger.warn("Participant file not found: " + participantFile);
                                                    System.out.println("Participant file not found. Continuing with organizer file only.");
                                                    workingParticipants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                                } else {
                                                    // Auto-merge the files with ID generation
                                                    String timestamp = String.valueOf(System.currentTimeMillis());
                                                    String tempMergedPath = "C:\\Users\\DELL\\Desktop\\merged_participants_" + timestamp + ".csv";

                                                    logger.info("Auto-merging files: " + uploadedFilePath + " with " + participantFile);
                                                    System.out.println("Merging files and generating IDs for new participants...");
                                                    workingParticipants = CSVMerger.mergeCSVFiles(uploadedFilePath, participantFile, tempMergedPath);
                                                    uploadedFilePath = tempMergedPath; // Update to use merged file
                                                    currentUploadedFilePath = tempMergedPath; // Update for participant login
                                                    logger.info("Files merged successfully. Total participants: " + workingParticipants.size());
                                                    System.out.println("✓ Successfully merged files! Total participants: " + workingParticipants.size());

                                                    // Show sample of new IDs assigned
                                                    System.out.println("\nSample of merged participants:");
                                                    for (int i = 0; i < Math.min(5, workingParticipants.size()); i++) {
                                                        Participant p = workingParticipants.get(i);
                                                        System.out.println("  " + p.getId() + " | " + p.getName() + " | " + p.getEmail());
                                                    }
                                                }
                                            } catch (Exception e) {
                                                logger.error("Auto-merge failed", e);
                                                System.out.println("Auto-merge failed: " + e.getMessage());
                                                System.out.println("Continuing with organizer file only...");
                                                workingParticipants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                            }
                                        } else {
                                            System.out.println("No file path provided. Continuing with organizer file only.");
                                            workingParticipants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                        }
                                    } else {
                                        // Load without merging
                                        try {
                                            workingParticipants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                            logger.info("Loaded participants without merging: " + workingParticipants.size());
                                            System.out.println("Loaded " + workingParticipants.size() + " participants from organizer file.");
                                        } catch (Exception e) {
                                            logger.error("Error loading participants", e);
                                            System.out.println("Error loading participants: " + e.getMessage());
                                            break;
                                        }
                                    }

                                    // CONTINUE DIRECTLY TO TEAM FORMATION
                                    System.out.println("\n" + "=".repeat(50));
                                    System.out.println("PROCEEDING TO TEAM FORMATION");
                                    System.out.println("=".repeat(50));

                                    // TEAM SIZE INPUT
                                    int teamSize = 0;
                                    System.out.print("Enter desired team size: ");
                                    try {
                                        teamSize = sc.nextInt();
                                        sc.nextLine();

                                        // Validate team size
                                        if (teamSize <= 0) {
                                            System.out.println("Team size must be greater than 0.");
                                            break;
                                        }
                                        if (teamSize > workingParticipants.size()) {
                                            System.out.println("Team size cannot be larger than total participants (" + workingParticipants.size() + ").");
                                            break;
                                        }

                                    } catch (InputMismatchException e) {
                                        System.out.println("Invalid input. Please enter a number.");
                                        sc.nextLine();
                                        break;
                                    }

                                    // TEAM FORMATION LOOP
                                    boolean arranging = true;
                                    while (arranging) {
                                        try {
                                            logger.info("Starting team formation with " + workingParticipants.size() + " participants, team size: " + teamSize);
                                            System.out.println("\nForming teams with " + workingParticipants.size() + " participants...");

                                            teams = TeamBuilder.formTeams(workingParticipants, teamSize);
                                            remainingPool = TeamBuilder.getRemainingParticipants();

                                            // DISPLAY MAIN TEAMS
                                            System.out.println("\n================== MAIN TEAMS ==================");
                                            if (teams.isEmpty()) {
                                                System.out.println("No complete teams could be formed with current constraints.");
                                            } else {
                                                for (int i = 0; i < teams.size(); i++) {
                                                    System.out.println("\n======= TEAM " + (i + 1) + " =======");
                                                    List<Participant> currentTeam = teams.get(i);
                                                    double teamAvgSkill = currentTeam.stream()
                                                            .mapToInt(Participant::getSkillLevel)
                                                            .average()
                                                            .orElse(0.0);

                                                    // Count roles and personalities for diversity info
                                                    long leaderCount = currentTeam.stream()
                                                            .filter(p -> p.getPersonalityType().name().equals("LEADER"))
                                                            .count();
                                                    long thinkerCount = currentTeam.stream()
                                                            .filter(p -> p.getPersonalityType().name().equals("THINKER"))
                                                            .count();

                                                    System.out.printf("Average Skill: %.2f | Size: %d | Leaders: %d | Thinkers: %d\n",
                                                            teamAvgSkill, currentTeam.size(), leaderCount, thinkerCount);

                                                    for (Participant p : currentTeam) {
                                                        System.out.println("  " + p.getId() + " | " + p.getName() +
                                                                " | " + p.getPreferredRole() +
                                                                " | Skill: " + p.getSkillLevel() +
                                                                " | " + p.getPersonalityType());
                                                    }
                                                }
                                            }

                                            // HANDLE LEFTOVER PARTICIPANTS
                                            List<List<Participant>> leftoverTeams = new ArrayList<>();
                                            if (!remainingPool.isEmpty()) {
                                                logger.info("Forming leftover teams from " + remainingPool.size() + " participants");
                                                System.out.println("\nForming teams from " + remainingPool.size() + " leftover participants...");
                                                leftoverTeams = TeamBuilder.formLeftoverTeams(teamSize);
                                                remainingPool = TeamBuilder.getRemainingParticipants();

                                                if (!leftoverTeams.isEmpty()) {
                                                    System.out.println("\n================== LEFTOVER TEAMS ==================");
                                                    int offset = teams.size();
                                                    for (int i = 0; i < leftoverTeams.size(); i++) {
                                                        System.out.println("\n======= TEAM " + (offset + i + 1) + " =======");
                                                        List<Participant> currentLeftoverTeam = leftoverTeams.get(i);
                                                        double teamAvgSkill = currentLeftoverTeam.stream()
                                                                .mapToInt(Participant::getSkillLevel)
                                                                .average()
                                                                .orElse(0.0);

                                                        System.out.printf("Average Skill: %.2f | Size: %d\n",
                                                                teamAvgSkill, currentLeftoverTeam.size());

                                                        for (Participant p : currentLeftoverTeam) {
                                                            System.out.println("  " + p.getId() + " | " + p.getName() +
                                                                    " | " + p.getPreferredRole() +
                                                                    " | Skill: " + p.getSkillLevel() +
                                                                    " | " + p.getPersonalityType());
                                                        }
                                                    }
                                                }
                                            }

                                            // DISPLAY REMAINING UNASSIGNED PARTICIPANTS
                                            if (!remainingPool.isEmpty()) {
                                                logger.info(remainingPool.size() + " participants remaining unassigned");
                                                System.out.println("\n================== REMAINING UNASSIGNED PARTICIPANTS ==================");
                                                System.out.println("Count: " + remainingPool.size());
                                                for (Participant p : remainingPool) {
                                                    System.out.println("  " + p.getId() + " | " + p.getName() +
                                                            " | " + p.getEmail() +
                                                            " | " + p.getPreferredGame() +
                                                            " | Skill: " + p.getSkillLevel());
                                                }
                                            }

                                            // REARRANGEMENT OPTION
                                            System.out.println("\n" + "=".repeat(50));
                                            System.out.print("Do you want to rearrange teams? (yes/no): ");
                                            String rearrange = sc.nextLine().trim().toLowerCase();
                                            if (rearrange.equals("yes") || rearrange.equals("y")) {
                                                // Collect all participants back for rearrangement
                                                workingParticipants = new ArrayList<>();
                                                for (List<Participant> t : teams) workingParticipants.addAll(t);
                                                for (List<Participant> t : leftoverTeams) workingParticipants.addAll(t);
                                                workingParticipants.addAll(remainingPool);

                                                // Clear current team data
                                                teams.clear();
                                                leftoverTeams.clear();
                                                remainingPool.clear();
                                                TeamBuilder.getRemainingParticipants().clear();

                                                logger.info("Rearranging teams with " + workingParticipants.size() + " participants");
                                                System.out.println("\nRearranging teams with " + workingParticipants.size() + " participants...\n");
                                            } else {
                                                // Combine main teams and leftover teams
                                                if (leftoverTeams != null && !leftoverTeams.isEmpty()) {
                                                    teams.addAll(leftoverTeams);
                                                }
                                                arranging = false;

                                                // Show final summary
                                                logger.info("Team formation completed. Total teams: " + teams.size() + ", remaining: " + remainingPool.size());
                                                System.out.println("\n" + "=".repeat(60));
                                                System.out.println("✓ TEAM FORMATION COMPLETED SUCCESSFULLY!");
                                                System.out.println("=".repeat(60));
                                                System.out.println("Total teams formed: " + teams.size());
                                                System.out.println("Total participants in teams: " + teams.stream().mapToInt(List::size).sum());
                                                System.out.println("Remaining unassigned: " + remainingPool.size());
                                                System.out.println("Merged participants used: " + workingParticipants.size());
                                            }

                                        } catch (Exception e) {
                                            logger.error("Error forming teams", e);
                                            System.out.println("Error forming teams: " + e.getMessage());
                                            e.printStackTrace();
                                            arranging = false;
                                        }
                                    }
                                    break;
                                case 4:
                                    if (teams == null || teams.isEmpty()) {
                                        System.out.println("Teams not formed yet. Please form teams first (Option 3).");
                                    } else {
                                        Scanner saveScanner = new Scanner(System.in);

                                        System.out.println("\nEnter the FULL file path where you want to save the team CSV:");
                                        System.out.println("(Example: C:\\Users\\DELL\\Desktop\\final_teams.csv)");
                                        System.out.print("Path: ");
                                        String userPath = saveScanner.nextLine().trim();

                                        if (userPath.isEmpty()) {
                                            System.out.println("Invalid path. Saving cancelled.");
                                            break;
                                        }

                                        // Ensure file ends with .csv
                                        if (!userPath.toLowerCase().endsWith(".csv")) {
                                            userPath = userPath + ".csv";
                                        }

                                        try {
                                            TeamFileHandler.saveTeamsToCSV(teams, userPath);
                                            // Update the TEAMS_OUTPUT_PATH with the user-provided path
                                            TEAMS_OUTPUT_PATH = userPath;
                                            logger.info("Teams saved successfully to: " + TEAMS_OUTPUT_PATH);
                                            System.out.println("\n✔ Teams successfully saved!");
                                            System.out.println("Saved to: " + TEAMS_OUTPUT_PATH);
                                            System.out.println("Total formatted teams saved: " + teams.size());
                                            System.out.println("Participants can now view their assigned teams in the participant menu.");
                                        } catch (Exception e) {
                                            logger.error("Error saving teams to: " + userPath, e);
                                            System.out.println(" Error saving teams: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 5:
                                    try {
                                        // Use the updated TEAMS_OUTPUT_PATH variable instead of hardcoded OUTPUT_PATH
                                        File teamFile = new File(TEAMS_OUTPUT_PATH);
                                        if (!teamFile.exists()) {
                                            System.out.println("No team assignments found. Please form and save teams first.");
                                        } else {
                                            List<Participant> teamAssignments = FileHandler.loadTeamsFromOutput(TEAMS_OUTPUT_PATH);
                                            if (teamAssignments.isEmpty()) {
                                                System.out.println("No team assignments found in the file.");
                                            } else {
                                                logger.info("Viewing team assignments from: " + TEAMS_OUTPUT_PATH);
                                                System.out.println("\n--- CURRENT TEAM ASSIGNMENTS ---");
                                                // Group by team number
                                                Map<String, List<Participant>> teamsMap = teamAssignments.stream()
                                                        .collect(Collectors.groupingBy(Participant::getTeamNumber));

                                                for (Map.Entry<String, List<Participant>> entry : teamsMap.entrySet()) {
                                                    System.out.println("\nTeam " + entry.getKey() + " (" + entry.getValue().size() + " members):");
                                                    for (Participant member : entry.getValue()) {
                                                        System.out.println("  " + member.getName() + " (" + member.getId() + ") - " +
                                                                member.getPreferredRole() + " - " + member.getPersonalityType());
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("Error loading team assignments", e);
                                        System.out.println("Error loading team assignments: " + e.getMessage());
                                    }
                                    break;

                                case 6:
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

                // ============================= EXIT SYSTEM =============================
                else if (loginChoice == 3) {
                    logger.info("Application exiting");
                    System.out.println("\nExiting system... Goodbye!");
                    sc.close();
                    System.exit(0);
                } else {
                    logger.warn("Invalid login option selected: " + loginChoice);
                    System.out.println("Invalid Login Option!");
                }

            } catch (Exception e) {
                logger.error("An unexpected error occurred in main loop", e);
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    // Helper method to display team statistics
    private static void displayTeamStats(List<Participant> team) {
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

    // Helper method to format map for display
    private static String formatMap(Map<String, Long> map) {
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }
}