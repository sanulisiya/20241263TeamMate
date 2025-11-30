package cli;

import model.Participant;
import service.*;
import utility.LoggerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TeamFormationHandler {
    private static final LoggerService logger = LoggerService.getInstance();
    private Scanner scanner;
    private String uploadedFilePath;
    private String teamsOutputPath;

    public TeamFormationHandler(Scanner scanner, String uploadedFilePath, String teamsOutputPath) {
        this.scanner = scanner;
        this.uploadedFilePath = uploadedFilePath;
        this.teamsOutputPath = teamsOutputPath;
    }

    public TeamFormationResult handleTeamFormation() {
        // Check if we have uploaded file
        if (uploadedFilePath == null) {
            System.out.println("No file uploaded. Please upload a CSV first.");
            return null;
        }

        // ENHANCED MERGE FUNCTIONALITY
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" DATA MERGE OPTIONS");
        System.out.println("=".repeat(60));

        int newParticipantsCount = CSVMerger.getNewParticipantsCount();
        System.out.println(" Current Status:");
        System.out.println("   - Organizer file: " + uploadedFilePath);
        System.out.println("   - New participants waiting: " + newParticipantsCount);

        System.out.print("\nDo you want to merge additional data? (yes/no): ");
        String mergeChoice = scanner.nextLine().trim().toLowerCase();

        List<Participant> workingParticipants = handleEnhancedMergeChoice(mergeChoice);
        if (workingParticipants == null || workingParticipants.isEmpty()) {
            System.out.println("No participants available for team formation.");
            return null;
        }

        // TEAM SIZE INPUT
        int teamSize = getTeamSize(workingParticipants);
        if (teamSize <= 0) return null;

        return performTeamFormation(workingParticipants, teamSize);
    }

    private List<Participant> handleEnhancedMergeChoice(String mergeChoice) {
        List<Participant> workingParticipants = new ArrayList<>();

        if (mergeChoice.equals("yes") || mergeChoice.equals("y")) {
            try {
                // Create merged file on Desktop
                String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
                String timestamp = String.valueOf(System.currentTimeMillis());
                String tempMergedPath = desktopPath + File.separator + "merged_participants_" + timestamp + ".csv";

                logger.info("Starting simple merge process");
                System.out.println("\n Starting merge process...");
                System.out.println(" Merged file will be saved to: " + tempMergedPath);

                workingParticipants = CSVMerger.mergeWithOptions(uploadedFilePath, tempMergedPath, scanner);

                uploadedFilePath = tempMergedPath; // Update to use merged file
                logger.info("Merge completed. Total participants: " + workingParticipants.size());
                System.out.println(" Merge completed! Total participants: " + workingParticipants.size());

            } catch (Exception e) {
                logger.error("Merge failed", e);
                System.out.println(" Merge failed: " + e.getMessage());
                System.out.println("Continuing with organizer file only...");
                workingParticipants = loadOrganizerFileOnly();
            }
        } else {
            // Load without merging
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

            // Validate team size
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

    private TeamFormationResult performTeamFormation(List<Participant> workingParticipants, int teamSize) {
        List<List<Participant>> teams = null;
        List<Participant> remainingPool = new ArrayList<>();

        boolean arranging = true;
        while (arranging) {
            try {
                logger.info("Starting team formation with " + workingParticipants.size() + " participants, team size: " + teamSize);
                System.out.println("\nForming teams with " + workingParticipants.size() + " participants...");

                teams = TeamBuilder.formTeams(workingParticipants, teamSize);
                remainingPool = TeamBuilder.getRemainingParticipants();

                // DISPLAY MAIN TEAMS
                System.out.println("\n ================== MAIN TEAMS ==================");
                if (teams.isEmpty()) {
                    System.out.println("No complete teams could be formed with current constraints.");
                } else {
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println("\n--------------------------------------------------------------------------------------------------------------------------");
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

                // HANDLE LEFTOVER PARTICIPANTS
                List<List<Participant>> leftoverTeams = new ArrayList<>();
                if (!remainingPool.isEmpty()) {
                    logger.info("Forming leftover teams from " + remainingPool.size() + " participants");
                    System.out.println("\nForming teams from " + remainingPool.size() + " leftover participants...");
                    leftoverTeams = TeamBuilder.formLeftoverTeams(teamSize);
                    remainingPool = TeamBuilder.getRemainingParticipants();

                    if (!leftoverTeams.isEmpty()) {
                        System.out.println("\nFORMING LEFTOVER TEAMS - Note: No strict rules followed in leftover team formation");
                        System.out.println("\n===================== LEFTOVER TEAMS ======================");
                        int offset = teams.size();
                        for (int i = 0; i < leftoverTeams.size(); i++) {
                            System.out.println("\n======== TEAM " + (offset + i + 1) + " ========");
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
                String rearrange = scanner.nextLine().trim().toLowerCase();
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
                    System.out.println("\n Rearranging teams with " + workingParticipants.size() + " participants...\n");
                } else {
                    // Combine main teams and leftover teams
                    if (leftoverTeams != null && !leftoverTeams.isEmpty()) {
                        teams.addAll(leftoverTeams);
                    }
                    arranging = false;

                    // Show final summary
                    logger.info("Team formation completed. Total teams: " + teams.size() + ", remaining: " + remainingPool.size());
                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("TEAM FORMATION COMPLETED SUCCESSFULLY!");
                    System.out.println("=".repeat(60));
                    System.out.println("Total teams formed: " + teams.size());
                    System.out.println("Total participants in teams: " + teams.stream().mapToInt(List::size).sum());
                    System.out.println(" Remaining unassigned: " + remainingPool.size());
                    System.out.println(" Total participants processed: " + workingParticipants.size());
                }

            } catch (Exception e) {
                logger.error("Error forming teams", e);
                System.out.println(" Error forming teams: " + e.getMessage());
                e.printStackTrace();
                arranging = false;
            }
        }

        return new TeamFormationResult(teams, remainingPool, uploadedFilePath);
    }
}