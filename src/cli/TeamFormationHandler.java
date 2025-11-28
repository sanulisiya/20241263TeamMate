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
        // AUTO-MERGE FUNCTIONALITY
        System.out.print("\nDo you want to merge with participant-added data file? (yes/no): ");
        String mergeChoice = scanner.nextLine().trim().toLowerCase();

        List<Participant> workingParticipants = handleMergeChoice(mergeChoice);
        if (workingParticipants == null) return null;

        // TEAM SIZE INPUT
        int teamSize = getTeamSize(workingParticipants);
        if (teamSize <= 0) return null;

        return performTeamFormation(workingParticipants, teamSize);
    }

    private List<Participant> handleMergeChoice(String mergeChoice) {
        List<Participant> workingParticipants = new ArrayList<>();

        if (mergeChoice.equals("yes") || mergeChoice.equals("y")) {
            System.out.print("Enter path to participant-added CSV file: ");
            String participantFile = scanner.nextLine().trim();

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
                return null;
            }
        }
        return workingParticipants;
    }

    private int getTeamSize(List<Participant> workingParticipants) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("PROCEEDING TO TEAM FORMATION");
        System.out.println("=".repeat(50));

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
            return teamSize;
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Invalid input. Please enter a number.");
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
                        System.out.println("\nFORMING LEFTOVER TEAMS - Note: No strict rules followed in leftover team formationa");
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

        return new TeamFormationResult(teams, remainingPool, uploadedFilePath);
    }
}