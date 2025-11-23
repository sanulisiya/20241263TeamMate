import model.Participant;
import service.FileHandler;
import service.ParticipantCreator;
import service.TeamBuilder;
import service.TeamFileHandler;

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
                                    formattedTeams = Collections.singletonList(FileHandler.loadParticipantsSingleThread(OUTPUT_PATH));
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
                                        participants = FileHandler.loadParticipantsSingleThread(path);
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
                                            participants = FileHandler.loadParticipantsSingleThread(uploadedFilePath);
                                            System.out.println("\n--- PARTICIPANT LIST ---");
                                            for (Participant p : participants) {
                                                System.out.println(p);
                                            }
                                        } catch (Exception e) {
                                            System.out.println("Error loading participants: " + e.getMessage());
                                        }
                                    }
                                    break;

                                case 3: // Team Formation
                                    if (participants.isEmpty()) {
                                        System.out.println("Cannot form teams. Please load participants (Option 1) first.");
                                        break;
                                    }

                                    boolean keepArranging = true;

                                    while (keepArranging) {
                                        // Ask for team size
                                        int teamSize = 0;
                                        while (true) {
                                            System.out.print("Enter the desired team size (N): ");
                                            String input = sc.nextLine().trim();
                                            if (input.isEmpty()) {
                                                System.out.println("Please enter a valid number.");
                                                continue;
                                            }
                                            try {
                                                teamSize = Integer.parseInt(input);
                                                if (teamSize <= 1 || teamSize > participants.size()) {
                                                    System.out.println("Invalid team size. Must be > 1 and <= total participants (" + participants.size() + ").");
                                                } else {
                                                    break;
                                                }
                                            } catch (NumberFormatException e) {
                                                System.out.println("Please enter a valid number.");
                                            }
                                        }

                                        System.out.println("\nForming teams...");
                                        teams = TeamBuilder.formTeams(participants, teamSize);

                                        remainingPool = TeamBuilder.getRemainingParticipants();

                                        // Display formed teams
                                        if (!teams.isEmpty()) {
                                            System.out.println("\nTEAMS FORMED SUCCESSFULLY");
                                            int teamId = 1;
                                            for (List<Participant> team : teams) {
                                                System.out.printf("\n*** TEAM %d (Size: %d) ***\n", teamId++, team.size());
                                                System.out.println("------------------------------------------------");
                                                team.forEach(p -> System.out.println("  " + p));
                                            }
                                            TeamBuilder.printFormationStats(teams, participants);
                                        } else {
                                            System.out.println("\nCould not form any complete teams with size " + teamSize + ".");
                                        }

                                        // Show remaining
                                        if (!remainingPool.isEmpty()) {
                                            System.out.println("\nREMAINING PARTICIPANTS (could not fit into full teams):");
                                            remainingPool.forEach(p -> System.out.println("  " + p.getName() + " (Skill: " + p.getSkillLevel() + ")"));
                                        }

                                        // Ask if user wants to rearrange
                                        while (true) {
                                            System.out.print("\nDo you want to REARRANGE teams with a new arrangement? (yes/no): ");
                                            String answer = sc.nextLine().trim().toLowerCase();
                                            if (answer.equals("yes") || answer.equals("y")) {
                                                System.out.println("Starting fresh team formation...\n");
                                                TeamBuilder.clearRemainingParticipants(); // Clean internal state
                                                break; // Loop again — ask for team size again
                                            } else if (answer.equals("no") || answer.equals("n")) {
                                                System.out.println("Teams finalized! Returning to main menu.\n");
                                                keepArranging = false;
                                                break;
                                            } else {
                                                System.out.println("Please type 'yes' or 'no'.");
                                            }
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
                } else {
                    System.out.println("Invalid Login Option!");
                }

            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }

        sc.close();
    }
}
