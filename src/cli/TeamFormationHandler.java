package cli;

import model.Participant;
import model.Team; // Keep import for potential future use with a refined Team model
import service.CSVMerger;
import service.FileHandler;
import service.TeamBuilder;
import utility.LoggerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class TeamFormationHandler {
    private static final LoggerService logger = LoggerService.getInstance();
    private final Scanner scanner;
    private String uploadedFilePath;
    private final String teamsOutputPath;

    public TeamFormationHandler(Scanner scanner, String uploadedFilePath, String teamsOutputPath) {
        this.scanner = scanner;
        this.uploadedFilePath = uploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
    }

    public TeamFormationResult handleTeamFormation() {
        if (uploadedFilePath == null) {
            System.out.println("No file uploaded. Please upload a CSV first.");
            return null;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println(" DATA MERGE OPTIONS");
        System.out.println("=".repeat(60));
        System.out.println(" Current Status:");
        System.out.println("   - Organizer file: " + uploadedFilePath);
        System.out.println("   - New participants waiting: " + CSVMerger.getNewParticipantsCount());

        System.out.print("\nDo you want to merge additional data? (yes/no): ");
        String mergeChoice = scanner.nextLine().trim().toLowerCase();

        List<Participant> workingParticipants = handleEnhancedMergeChoice(mergeChoice);
        if (workingParticipants == null || workingParticipants.isEmpty()) {
            System.out.println("No participants available for team formation.");
            return null;
        }

        int teamSize = getTeamSize(workingParticipants);
        if (teamSize <= 0) return null;

        return performTeamFormation(workingParticipants, teamSize);
    }

    private List<Participant> handleEnhancedMergeChoice(String mergeChoice) {
        List<Participant> workingParticipants;
        if (mergeChoice.equals("yes") || mergeChoice.equals("y")) {
            try {
                String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
                String timestamp = String.valueOf(System.currentTimeMillis());
                String tempMergedPath = desktopPath + File.separator + "merged_participants_" + timestamp + ".csv";

                logger.info("Starting merge process. Merged file path: " + tempMergedPath);
                System.out.println("\n Starting merge process...");
                System.out.println(" Merged file will be saved to: " + tempMergedPath);

                workingParticipants = CSVMerger.mergeWithOptions(uploadedFilePath, tempMergedPath, scanner);

                uploadedFilePath = tempMergedPath;
                logger.info("Merge completed. Total participants: " + workingParticipants.size());
                System.out.println(" Merge completed! Total participants: " + workingParticipants.size());

            } catch (Exception e) {
                logger.error("Merge failed", e);
                System.out.println(" Merge failed: " + e.getMessage());
                System.out.println("Continuing with organizer file only...");
                workingParticipants = loadOrganizerFileOnly();
            }
        } else {
            workingParticipants = loadOrganizerFileOnly();
        }
        return workingParticipants;
    }

    private List<Participant> loadOrganizerFileOnly() {
        try {
            List<Participant> participants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
            logger.info("Loaded participants without merging: " + participants.size());
            System.out.println("Loaded " + participants.size() + " participants from organizer file.");
            return participants;
        } catch (Exception e) {
            logger.error("Error loading organizer file", e);
            System.out.println("Error loading participants: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int getTeamSize(List<Participant> workingParticipants) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(" PROCEEDING TO TEAM FORMATION");
        System.out.println("=".repeat(50));
        System.out.println("Total participants available: " + workingParticipants.size());

        System.out.print("Enter desired team size: ");
        try {
            int teamSize = scanner.nextInt();
            scanner.nextLine();

            if (teamSize <= 0) {
                System.out.println("Team size must be greater than 0.");
                return -1;
            }
            if (teamSize > workingParticipants.size()) {
                System.out.println("Team size cannot be larger than total participants (" + workingParticipants.size() + ").");
                return -1;
            }

            int maxPossibleTeams = workingParticipants.size() / teamSize;
            System.out.println("Maximum possible teams: " + maxPossibleTeams);

            return teamSize;
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println(" Invalid input. Please enter a number.");
            return -1;
        }
    }

    // ============================================================
    //              CLEANED TEAM FORMATION LOGIC
    // ============================================================

    private TeamFormationResult performTeamFormation(List<Participant> workingParticipants, int teamSize) {

        List<List<Participant>> finalTeams = new ArrayList<>();
        List<Participant> rearrangementPool = workingParticipants;
        boolean arranging = true;

        while (arranging) {
            try {
                logger.info("Attempting formation with " + rearrangementPool.size() + " participants, size: " + teamSize);
                System.out.println("\nForming teams with " + rearrangementPool.size() + " participants...");

                // 1. MAIN TEAM FORMATION
                List<List<Participant>> mainTeams = TeamBuilder.formTeams(rearrangementPool, teamSize);
                List<Participant> remainingPool = TeamBuilder.getRemainingParticipants(); // Static state retrieved

                // 2. LEFTOVER TEAM FORMATION
                List<List<Participant>> leftoverTeams = new ArrayList<>();
                if (!remainingPool.isEmpty()) {
                    logger.info("Forming leftover teams from " + remainingPool.size() + " participants");
                    System.out.println("\nAttempting to form leftover teams from " + remainingPool.size() + " participants...");
                    leftoverTeams = TeamBuilder.formLeftoverTeams(teamSize);
                    remainingPool = TeamBuilder.getRemainingParticipants(); // Static state updated
                }

                // 3. DISPLAY RESULTS (Using helper method)
                displayFormationResults(mainTeams, leftoverTeams, remainingPool, finalTeams.size());

                // 4. REARRANGE OPTION
                System.out.println("\n" + "=".repeat(50));
                System.out.print("Do you want to **rearrange all participants** to try for a better result? (yes/no): ");
                String rearrange = scanner.nextLine().trim().toLowerCase();

                if (rearrange.equals("yes") || rearrange.equals("y")) {
                    rearrangementPool = new ArrayList<>();
                    rearrangementPool.addAll(mainTeams.stream().flatMap(List::stream).collect(Collectors.toList()));
                    rearrangementPool.addAll(leftoverTeams.stream().flatMap(List::stream).collect(Collectors.toList()));
                    rearrangementPool.addAll(remainingPool);

                    // Clear static list before the next TeamBuilder run
                    TeamBuilder.getRemainingParticipants().clear();

                    logger.info("Rearranging teams with " + rearrangementPool.size() + " participants");
                    System.out.println("\n Rearranging teams with " + rearrangementPool.size() + " participants...\n");
                    // Continue loop
                } else {
                    finalTeams.addAll(mainTeams);
                    finalTeams.addAll(leftoverTeams);

                    // Set final remaining pool before exiting
                    rearrangementPool = remainingPool;
                    arranging = false;

                    // Show final summary
                    logger.info("Team formation completed. Total teams: " + finalTeams.size() + ", remaining: " + rearrangementPool.size());
                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("TEAM FORMATION COMPLETED SUCCESSFULLY!");
                    System.out.println("=".repeat(60));
                    System.out.println("Total teams formed: " + finalTeams.size());
                    System.out.println("Total participants in teams: " + finalTeams.stream().mapToInt(List::size).sum());
                    System.out.println(" Remaining unassigned: " + rearrangementPool.size());
                    System.out.println(" Total participants processed: " + workingParticipants.size());
                }

            } catch (Exception e) {
                logger.error("Error forming teams", e);
                System.out.println(" Error forming teams: " + e.getMessage());
                arranging = false;
            }
        }

        return new TeamFormationResult(finalTeams, rearrangementPool, uploadedFilePath);
    }

    //Helper method to handle console output for formation results.
    private void displayFormationResults(List<List<Participant>> mainTeams, List<List<Participant>> leftoverTeams, List<Participant> remainingPool, int offset) {

        // 1. MAIN TEAMS
        System.out.println("\n ================== MAIN TEAMS ==================");
        if (mainTeams.isEmpty()) {
            System.out.println("No complete teams could be formed with current constraints.");
        } else {
            for (int i = 0; i < mainTeams.size(); i++) {
                List<Participant> currentTeam = mainTeams.get(i);
                double teamAvgSkill = currentTeam.stream()
                        .mapToInt(Participant::getSkillLevel)
                        .average()
                        .orElse(0.0);

                // Count roles and personalities for diversity info (from the "new code")
                long leaderCount = currentTeam.stream()
                        .filter(p -> p.getPersonalityType().name().equals("LEADER"))
                        .count();
                long thinkerCount = currentTeam.stream()
                        .filter(p -> p.getPersonalityType().name().equals("THINKER"))
                        .count();

                System.out.println("\n--------------------------------------------------------------------------------------------------------------------------");
                System.out.println("\n======= TEAM " + (i + 1 + offset) + " =======");
                System.out.printf(" Average Skill: %.2f | Size: %d | Leaders: %d | Thinkers: %d\n",
                        teamAvgSkill, currentTeam.size(), leaderCount, thinkerCount);

                for (Participant p : currentTeam) {
                    System.out.println("  " + p.getId() + " | " + p.getName() +
                            " | " + p.getPreferredRole() +
                            " | Skill: " + p.getSkillLevel() +
                            " | " + p.getPersonalityType());
                }
            }
        }

        // 2. LEFTOVER TEAMS
        if (!leftoverTeams.isEmpty()) {
            System.out.println("\nFORMING LEFTOVER TEAMS - Note: No strict rules followed in leftover team formation");
            System.out.println("\n===================== LEFTOVER TEAMS ======================");
            int mainTeamSize = mainTeams.size();
            for (int i = 0; i < leftoverTeams.size(); i++) {
                List<Participant> currentLeftoverTeam = leftoverTeams.get(i);
                double teamAvgSkill = currentLeftoverTeam.stream()
                        .mapToInt(Participant::getSkillLevel)
                        .average()
                        .orElse(0.0);

                System.out.println("\n======== TEAM " + (mainTeamSize + i + 1 + offset) + " ========");
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

        // 3. UNASSIGNED PARTICIPANTS
        if (!remainingPool.isEmpty()) {
            System.out.println("\n================== REMAINING UNASSIGNED PARTICIPANTS ==================");
            System.out.println("Count: " + remainingPool.size());
            for (Participant p : remainingPool) {
                System.out.println("  " + p.getId() + " | " + p.getName() +
                        " | " + p.getEmail() +
                        " | " + p.getPreferredGame() +
                        " | Skill: " + p.getSkillLevel());
            }
        }
    }
}