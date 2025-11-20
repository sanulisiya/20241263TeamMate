package Main;

import model.Participant;
import Service.FileHandler;
import Service.Pcreator;
import Service.TeamBuilder;
import Service.TeamFileHandler;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class MainCLI {

    // NOTE: Replace these with actual, valid file paths on your system
    private static final String FILE_PATH = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
    private static final String OUTPUT_PATH = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";

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
                            Pcreator.createNewParticipant(FILE_PATH);
                            System.out.println("\nParticipant added successfully!");
                        } catch (Exception e) {
                            System.out.println("Failed to add participant: " + e.getMessage());
                        }
                        System.out.println("Returning to main menu...\n");
                        continue;
                    }

                    // ---- EXISTING PARTICIPANT LOGIN (Uses Multi-Threaded FileHandler) ----
                    else if (participantChoice == 2) {
                        System.out.print("Enter Participant ID: ");
                        String participantId = sc.nextLine().trim();
                        try {
                            participants = FileHandler.loadParticipants(FILE_PATH);
                        } catch (Exception e) {
                            System.out.println("Error loading participants: " + e.getMessage());
                            continue;
                        }

                        Participant found = null;
                        for (Participant p : participants) {
                            if (p.getId().equalsIgnoreCase(participantId)) {
                                found = p;
                                break;
                            }
                        }

                        if (found != null) {
                            System.out.println("\nParticipant Found!");
                            System.out.println(found);

                            System.out.print("\nDo you want to view your assigned team? (yes/no): ");
                            String yn = sc.nextLine().trim().toLowerCase();

                            if (yn.equals("yes")) {
                                List<List<Participant>> formattedTeams;
                                try {
                                    formattedTeams = FileHandler.loadFormattedTeams(OUTPUT_PATH);
                                } catch (Exception e) {
                                    System.out.println("Error loading teams: " + e.getMessage());
                                    continue;
                                }

                                if (formattedTeams == null || formattedTeams.isEmpty()) {
                                    System.out.println("\nTeams have not been formed yet. Please check later!");
                                } else {
                                    boolean foundTeam = false;
                                    for (int i = 0; i < formattedTeams.size(); i++) {
                                        List<Participant> team = formattedTeams.get(i);
                                        for (Participant teammate : team) {
                                            if (teammate.getId().equalsIgnoreCase(found.getId())) {
                                                System.out.println("\nYou are in TEAM " + (i + 1));
                                                System.out.println("---- Team Members ----");
                                                for (Participant t : team) {
                                                    System.out.println(t);
                                                }
                                                foundTeam = true;
                                                break;
                                            }
                                        }
                                        if (foundTeam) break;
                                    }
                                    if (!foundTeam) {
                                        System.out.println("\nYou are not assigned to any team yet.");
                                    }
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
                    boolean organizerRunning = true;
                    String uploadedFilePath = null;

                    while (organizerRunning) {
                        try {
                            System.out.println("\n------ ORGANIZER PANEL -------");
                            System.out.println("1. Upload CSV");
                            System.out.println("2. View All Participants");
                            System.out.println("3. Formation of Teams");
                            System.out.println("4. Save Formed Teams");
                            System.out.println("5. Back to Main Menu");
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
                                        // Uses the multi-threaded FileHandler
                                        participants = FileHandler.loadParticipants(path);
                                        if (participants != null && !participants.isEmpty()) {
                                            uploadedFilePath = path;
                                            System.out.println("CSV Uploaded Successfully! Total Participants: " + participants.size());
                                        } else {
                                            System.out.println("CSV Upload Failed. Check file path!");
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Error uploading CSV: " + e.getMessage());
                                    }
                                    break;

                                case 2:
                                    if (uploadedFilePath == null) {
                                        System.out.println("No file uploaded. Please upload a CSV first.");
                                    } else {
                                        try {
                                            // Uses the multi-threaded FileHandler
                                            participants = FileHandler.loadParticipants(uploadedFilePath);
                                            System.out.println("\n--- PARTICIPANT LIST ---");
                                            for (Participant p : participants) {
                                                System.out.println(p);
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
                                        // Uses the multi-threaded FileHandler
                                        participants = FileHandler.loadParticipants(uploadedFilePath);
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
                                            // Uses the multi-threaded TeamBuilder
                                            teams = TeamBuilder.formTeams(participants, teamSize);
                                            remainingPool = TeamBuilder.getRemainingParticipants();

                                            System.out.println("\n================== MAIN TEAMS ==================");
                                            for (int i = 0; i < teams.size(); i++) {
                                                System.out.println("\n------------------------------------------------------------------------------------------------");
                                                System.out.println("\n======= TEAM " + (i + 1) + " =======");
                                                for (Participant p : teams.get(i)) {
                                                    System.out.println(p);
                                                }
                                            }

                                            // Form leftover teams
                                            List<List<Participant>> leftoverTeams = new ArrayList<>();
                                            if (!remainingPool.isEmpty()) {
                                                leftoverTeams = TeamBuilder.formLeftoverTeams(teamSize);
                                                remainingPool = TeamBuilder.getRemainingParticipants();

                                                if (!leftoverTeams.isEmpty()) {
                                                    System.out.println("\n\"The initial strict team-building rules assigned the core teams." +
                                                            " \nThe following Leftover Teams were formed by prioritizing team size and overall skill balance," +
                                                            " \nrelaxing strict rules like role limits and game caps to accommodate the remaining participants.");

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

                                            // Remaining unassigned
                                            if (!remainingPool.isEmpty()) {
                                                System.out.println("\nRemaining Unassigned Participants:");
                                                for (Participant p : remainingPool) {
                                                    System.out.println(p);
                                                }
                                            }

                                            // Ask for rearrangement
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
                                                // Merge all teams for saving
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
                                        System.out.println("Teams not formed yet. Please form teams first.");
                                    } else {
                                        try {
                                            TeamFileHandler.saveTeamsToCSV(teams, OUTPUT_PATH);
                                            System.out.println("\nTeams saved to: " + OUTPUT_PATH);
                                        } catch (Exception e) {
                                            System.out.println("Error saving teams: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 5:
                                    organizerRunning = false;
                                    break;

                                default:
                                    System.out.println("Invalid option. Try again!");
                                    break;
                            }

                        } catch (Exception e) {
                            System.out.println("An error occurred in organizer panel: " + e.getMessage());
                        }
                    }
                }

                // ============================= EXIT SYSTEM =============================
                else if (loginChoice == 3) {
                    System.out.println("\nExiting system... Goodbye!");
                    break;
                }

                else {
                    System.out.println("Invalid Login Option!");
                }

            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }

        sc.close();
    }
}