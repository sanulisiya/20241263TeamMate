import model.Participant;
import service.FileHandler;
import service.ParticipantCreator;
import service.TeamBuilder;
import service.TeamFileHandler;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MainCLI {

    private static final String FILE_PATH = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
    private static final String OUTPUT_PATH = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";

    // Organizer login PIN
    private static final String ORGANIZER_PIN = "1234";

    public static void main(String[] args) {

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
                    System.out.println("\n--- PARTICIPANT MENU ---");
                    System.out.println("1. Add New Participant");
                    System.out.println("2. Login as Existing Participant");
                    System.out.print("Select option: ");

                    int participantChoice = 0;
                    try {
                        participantChoice = sc.nextInt();
                        sc.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine();
                        continue;
                    }

                    // ---- ADD NEW PARTICIPANT ----
                    if (participantChoice == 1) {
                        try {
                            ParticipantCreator.createNewParticipant(FILE_PATH);
                            System.out.println("\nParticipant added successfully!");
                        } catch (Exception e) {
                            System.out.println("Failed to add participant: " + e.getMessage());
                            e.printStackTrace();
                        }
                        System.out.println("Returning to main menu...\n");
                        continue;
                    }

                    // ---- EXISTING PARTICIPANT LOGIN ----
                    else if (participantChoice == 2) {
                        System.out.print("Enter Participant ID :  ");
                        String participantId = sc.nextLine().trim();

                        try {
                            participants = FileHandler.loadParticipantsSingleThread(FILE_PATH);
                        } catch (Exception e) {
                            System.out.println("Error loading participants: " + e.getMessage());
                            continue;
                        }

                        Participant found = participants.stream()
                                .filter(p -> p.getId().equalsIgnoreCase(participantId))
                                .findFirst()
                                .orElse(null);

                        if (found != null) {
                            System.out.println("\nParticipant Found!");
                            System.out.println(found);

                            System.out.print("\nDo you want to view your assigned team? (yes/no): ");
                            String yn = sc.nextLine().trim().toLowerCase();

                            if (yn.equals("yes") || yn.equals("y")) {
                                try {
                                    // Check if team file exists
                                    File teamFile = new File(OUTPUT_PATH);
                                    if (!teamFile.exists()) {
                                        System.out.println("\nNo teams have been formed yet. Please check later!");
                                        System.out.println("\nReturning to main menu...\n");
                                        continue;
                                    }

                                    // Load participants from team file using the correct method
                                    List<Participant> teamParticipants = FileHandler.loadTeamsFromOutput(OUTPUT_PATH);

                                    if (teamParticipants.isEmpty()) {
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
                                        System.out.println("\nYou are not assigned to any team yet.");
                                        System.out.println("\nReturning to main menu...\n");
                                        continue;
                                    }

                                    String teamNumber = teamParticipant.getTeamNumber();

                                    // Find all participants in the same team
                                    List<Participant> teamMembers = teamParticipants.stream()
                                            .filter(p -> teamNumber.equals(p.getTeamNumber()))
                                            .collect(Collectors.toList());

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
                                    System.out.println("Error loading teams: " + e.getMessage());
                                    System.out.println("Please make sure teams have been formed and saved by the organizer.");
                                }
                            }

                            System.out.println("\nReturning to main menu...\n");
                            continue;

                        } else {
                            System.out.println("\nParticipant not found. Returning to main menu...");
                            continue;
                        }
                    } else {
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
                        System.out.println(" Incorrect PIN. Returning to main menu...\n");
                        continue;
                    }

                    System.out.println("\n PIN Verified! Access Granted.");

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
                                            System.out.println("CSV Uploaded Successfully! Total Participants: " + participants.size());

                                            // Display sample of loaded participants
                                            System.out.println("\nSample of loaded participants:");
                                            for (int i = 0; i < Math.min(3, participants.size()); i++) {
                                                System.out.println("  " + participants.get(i));
                                            }
                                        } else {
                                            System.out.println("CSV Upload Failed. Check file path or file format!");
                                        }
                                    } catch (Exception e) {
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
                                            System.out.println("\n--- PARTICIPANT LIST ---");
                                            System.out.println("Total Participants: " + participants.size());
                                            for (int i = 0; i < participants.size(); i++) {
                                                System.out.println((i + 1) + ". " + participants.get(i));
                                            }
                                        } catch (Exception e) {
                                            System.out.println("Error loading participants: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 3:
                                    if (uploadedFilePath == null) {
                                        System.out.println("No file uploaded. Upload CSV first.");
                                        break;
                                    }

                                    try {
                                        participants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                    } catch (Exception e) {
                                        System.out.println("Error loading participants: " + e.getMessage());
                                        break;
                                    }

                                    int teamSize = 0;
                                    System.out.print("Enter desired team size: ");
                                    try {
                                        teamSize = sc.nextInt();
                                        sc.nextLine();
                                    } catch (InputMismatchException e) {
                                        System.out.println("Invalid input. Please enter a number.");
                                        sc.nextLine();
                                        break;
                                    }

                                    boolean arranging = true;
                                    while (arranging) {
                                        try {
                                            teams = TeamBuilder.formTeams(participants, teamSize);
                                            remainingPool = TeamBuilder.getRemainingParticipants();

                                            System.out.println("\n================== MAIN TEAMS ==================");
                                            for (int i = 0; i < teams.size(); i++) {
                                                System.out.println("\n======= TEAM " + (i + 1) + " =======");
                                                for (Participant p : teams.get(i)) {
                                                    System.out.println(p);
                                                }
                                            }

                                            // leftover
                                            List<List<Participant>> leftoverTeams = new ArrayList<>();
                                            if (!remainingPool.isEmpty()) {
                                                leftoverTeams = TeamBuilder.formLeftoverTeams(teamSize);
                                                remainingPool = TeamBuilder.getRemainingParticipants();

                                                if (!leftoverTeams.isEmpty()) {
                                                    System.out.println("\n================== LEFTOVER TEAMS ==================");
                                                    int offset = teams.size();
                                                    for (int i = 0; i < leftoverTeams.size(); i++) {
                                                        System.out.println("\n======= TEAM " + (offset + i + 1) + " =======");
                                                        for (Participant p : leftoverTeams.get(i)) {
                                                            System.out.println(p);
                                                        }
                                                    }
                                                }
                                            }

                                            if (!remainingPool.isEmpty()) {
                                                System.out.println("\nRemaining Unassigned Participants:");
                                                for (Participant p : remainingPool) {
                                                    System.out.println(p);
                                                }
                                            }

                                            System.out.print("\nDo you want to rearrange teams? (yes/no): ");
                                            String rearrange = sc.nextLine().trim().toLowerCase();
                                            if (rearrange.equals("yes")) {
                                                participants = new ArrayList<>();
                                                for (List<Participant> t : teams) participants.addAll(t);
                                                for (List<Participant> t : leftoverTeams) participants.addAll(t);
                                                participants.addAll(remainingPool);

                                                teams.clear();
                                                remainingPool.clear();
                                                TeamBuilder.getRemainingParticipants().clear();
                                                System.out.println("\nRearranging teams...\n");
                                            } else {
                                                teams.addAll(leftoverTeams);
                                                arranging = false;
                                            }

                                        } catch (Exception e) {
                                            System.out.println("Error forming teams: " + e.getMessage());
                                            arranging = false;
                                        }
                                    }
                                    break;

                                case 4:
                                    if (teams == null || teams.isEmpty()) {
                                        System.out.println("Teams not formed yet. Please form teams first (Option 3).");
                                    } else {
                                        try {
                                            TeamFileHandler.saveTeamsToCSV(teams, OUTPUT_PATH);
                                            System.out.println("\nTeams successfully saved to: " + OUTPUT_PATH);
                                            System.out.println("Total teams saved: " + teams.size());
                                            System.out.println("Participants can now view their assigned teams in the participant menu.");
                                        } catch (Exception e) {
                                            System.out.println(" Error saving teams: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 5:
                                    try {
                                        File teamFile = new File(OUTPUT_PATH);
                                        if (!teamFile.exists()) {
                                            System.out.println("No team assignments found. Please form and save teams first.");
                                        } else {
                                            List<Participant> teamAssignments = FileHandler.loadTeamsFromOutput(OUTPUT_PATH);
                                            if (teamAssignments.isEmpty()) {
                                                System.out.println("No team assignments found in the file.");
                                            } else {
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
                                        System.out.println("Error loading team assignments: " + e.getMessage());
                                    }
                                    break;

                                case 6:
                                    organizerRunning = false;
                                    System.out.println("Returning to main menu...");
                                    break;

                                default:
                                    System.out.println("Invalid option. Try again!");
                                    break;
                            }

                        } catch (Exception e) {
                            System.out.println("An error occurred in organizer panel: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                // ============================= EXIT SYSTEM =============================
                else if (loginChoice == 3) {
                    System.out.println("\nExiting system... Goodbye!");
                    sc.close();
                    System.exit(0);
                } else {
                    System.out.println("Invalid Login Option!");
                }

            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
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