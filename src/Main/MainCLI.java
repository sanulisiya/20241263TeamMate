package Main;

import model.Participant;
import Service.FileHandler;
import Service.Pcreator;
import Service.TeamBuilder;
import Service.TeamFileHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainCLI {

    private static final String FILE_PATH = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
    private static final String OUTPUT_PATH = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        List<Participant> participants = new ArrayList<>();
        List<List<Participant>> teams = null;
        List<Participant> remainingPool = new ArrayList<>();

        System.out.println("==== TEAMMATE COMMUNITY SYSTEM ====");
        System.out.println("Login as:");
        System.out.println("1. Participant");
        System.out.println("2. Organizer");
        System.out.print("Select option: ");

        int loginChoice = sc.nextInt();
        sc.nextLine();

        // ---------------- PARTICIPANT FLOW ----------------
        if (loginChoice == 1) {
            System.out.println("\n--- PARTICIPANT MENU ---");
            System.out.println("1. Add New Participant");
            System.out.println("2. Login as Existing Participant");
            System.out.print("Select option: ");

            int participantChoice = sc.nextInt();
            sc.nextLine();

            if (participantChoice == 1) {
                // Add new participant
                Pcreator.createNewParticipant(FILE_PATH);

            } else if (participantChoice == 2) {
                // Existing participant login
                System.out.print("Enter Participant ID: ");
                String participantId = sc.nextLine().trim();

                participants = FileHandler.loadParticipants(FILE_PATH);
                Participant found = null;
                for (Participant p : participants) {
                    if (p.getId().equalsIgnoreCase(participantId)) {
                        found = p;
                        break;
                    }
                }

                if (found != null) {
                    System.out.println("\n Participant Found!");
                    System.out.println(found);

                    boolean loginMenu = true;
                    while (loginMenu) {
                        System.out.println("\nDo you want to:");
                        System.out.println("1. View Assigned Team");
                        System.out.println("2. Exit");
                        System.out.print("Enter choice: ");
                        int choice = sc.nextInt();
                        sc.nextLine();

                        switch (choice) {
                            case 1:
                                List<List<Participant>> formattedTeams = FileHandler.loadFormattedTeams(OUTPUT_PATH);
                                if (formattedTeams == null || formattedTeams.isEmpty()) {
                                    System.out.println("\n Teams have not been formed yet. Please check later!");
                                } else {
                                    boolean foundTeam = false;
                                    for (int i = 0; i < formattedTeams.size(); i++) {
                                        List<Participant> team = formattedTeams.get(i);
                                        for (Participant member : team) {
                                            if (member.getId().equalsIgnoreCase(found.getId())) {
                                                System.out.println("\n You are in TEAM " + (i + 1));
                                                System.out.println("---- Team Members ----");
                                                for (Participant teammate : team) {
                                                    System.out.println(teammate);
                                                }
                                                foundTeam = true;
                                                break;
                                            }
                                        }
                                        if (foundTeam) break;
                                    }
                                    if (!foundTeam) {
                                        System.out.println("\n️ You are not assigned to any team yet. Please check later.");
                                    }
                                }
                                break;


                            case 2:
                                System.out.println("Logging out...");
                                loginMenu = false;
                                break;

                            default:
                                System.out.println("Invalid choice. Please enter 1 or 2.");
                        }
                    }

                } else {
                    System.out.println(" Participant not found. Please check your ID.");
                }
            } else {
                System.out.println(" Invalid option. Please enter 1 or 2.");
            }
        }

        // ---------------- ORGANIZER FLOW ----------------
        else if (loginChoice == 2) {
            boolean running = true;
            while (running) {
                System.out.println("\n--- ORGANIZER PANEL ---");
                System.out.println("1. Upload CSV");
                System.out.println("2. View All Participants");
                System.out.println("3. Formation of Teams");
                System.out.println("4. Save Formed Teams");
                System.out.println("5. Exit");
                System.out.print("Select option: ");

                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        // Upload CSV
                        System.out.print("\nEnter CSV File Path: ");
                        String path = sc.nextLine();
                        participants = FileHandler.loadParticipants(path);
                        if (participants != null && !participants.isEmpty()) {
                            System.out.println(" CSV Uploaded Successfully! Total Participants: " + participants.size());
                        } else {
                            System.out.println(" CSV Upload Failed. Check file path!");
                        }
                        break;

                    case 2:
                        // View All Participants
                        participants = FileHandler.loadParticipants(FILE_PATH);
                        if (participants == null || participants.isEmpty()) {
                            System.out.println("️ No participants available.");
                        } else {
                            System.out.println("\n--- PARTICIPANT LIST ---");
                            for (Participant p : participants) {
                                System.out.println(p);
                            }
                        }
                        break;

                    case 3:
                        // Formation of Teams
                        participants = FileHandler.loadParticipants(FILE_PATH);
                        if (participants == null || participants.isEmpty()) {
                            System.out.println("️ No participants found. Upload CSV first.");
                        } else {
                            System.out.print("Enter desired team size: ");
                            int teamSize = sc.nextInt();
                            sc.nextLine();

                            teams = TeamBuilder.formTeams(participants, teamSize);
                            remainingPool = TeamBuilder.getRemainingParticipants();

                            System.out.println("\n Teams Formed Successfully!");
                            for (int i = 0; i < teams.size(); i++) {
                                System.out.println("\n======= TEAM " + (i + 1) + " =======");
                                for (Participant p : teams.get(i)) {
                                    System.out.println(p);
                                }
                            }

                            if (!remainingPool.isEmpty()) {
                                System.out.println("\nRemaining Unassigned Participants:");
                                for (Participant p : remainingPool) {
                                    System.out.println(p);
                                }
                            } else {
                                System.out.println("\nAll participants successfully assigned to teams!");
                            }
                        }
                        break;

                    case 4:
                        // Save Formed Teams
                        if (teams == null || teams.isEmpty()) {
                            System.out.println("️ Teams not formed yet. Please form teams first.");
                        } else {
                            TeamFileHandler.saveTeamsToCSV(teams, OUTPUT_PATH);
                            System.out.println(" Teams saved to: " + OUTPUT_PATH);
                        }
                        break;

                    case 5:
                        // Exit
                        System.out.println("Organizer Logged Out Successfully!");
                        running = false;
                        break;

                    default:
                        System.out.println(" Invalid Option. Try again!");
                        break;
                }
            }
        } else {
            System.out.println(" Invalid Login Option!");
        }

        sc.close();
    }
}
