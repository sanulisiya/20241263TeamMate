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

        while (true) {

            System.out.println("\n==== TEAMMATE COMMUNITY SYSTEM ====");
            System.out.println("Login as:");
            System.out.println("1. Participant");
            System.out.println("2. Organizer");
            System.out.println("3. Exit");
            System.out.print("Select option: ");

            int loginChoice = sc.nextInt();
            sc.nextLine();

            // ============================= PARTICIPANT FLOW =============================
            if (loginChoice == 1) {

                System.out.println("\n--- PARTICIPANT MENU ---");
                System.out.println("1. Add New Participant");
                System.out.println("2. Login as Existing Participant");
                System.out.print("Select option: ");

                int participantChoice = sc.nextInt();
                sc.nextLine();

                // ---- ADD NEW PARTICIPANT ----
                if (participantChoice == 1) {

                    Pcreator.createNewParticipant(FILE_PATH);

                    System.out.println("\nParticipant added successfully!");
                    System.out.println("Returning to main menu...\n");
                    continue; // back to main menu

                }

                // ---- EXISTING PARTICIPANT LOGIN ----
                else if (participantChoice == 2) {

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

                        System.out.println("\nParticipant Found!");
                        System.out.println(found);

                        System.out.print("\nDo you want to view your assigned team? (yes/no): ");
                        String yn = sc.nextLine().trim().toLowerCase();

                        if (yn.equals("yes")) {

                            List<List<Participant>> formattedTeams = FileHandler.loadFormattedTeams(OUTPUT_PATH);

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
                        continue; // back to main menu

                    } else {
                        System.out.println("\nParticipant not found. Returning to main menu...");
                        continue;
                    }
                }

                else {
                    System.out.println("Invalid participant option.");
                    continue;
                }
            }

            // ============================= ORGANIZER FLOW =============================
            else if (loginChoice == 2) {

                boolean organizerRunning = true;

                while (organizerRunning) {

                    System.out.println("\n--- ORGANIZER PANEL ---");
                    System.out.println("1. Upload CSV");
                    System.out.println("2. View All Participants");
                    System.out.println("3. Formation of Teams");
                    System.out.println("4. Save Formed Teams");
                    System.out.println("5. Back to Main Menu");
                    System.out.print("Select option: ");

                    int choice = sc.nextInt();
                    sc.nextLine();

                    switch (choice) {

                        case 1:
                            System.out.print("\nEnter CSV File Path: ");
                            String path = sc.nextLine();

                            participants = FileHandler.loadParticipants(path);

                            if (participants != null && !participants.isEmpty()) {
                                System.out.println("CSV Uploaded Successfully! Total Participants: " + participants.size());
                            } else {
                                System.out.println("CSV Upload Failed. Check file path!");
                            }
                            break; // stay inside organizer menu

                        case 2:
                            participants = FileHandler.loadParticipants(FILE_PATH);

                            if (participants == null || participants.isEmpty()) {
                                System.out.println("No participants available.");
                            } else {
                                System.out.println("\n--- PARTICIPANT LIST ---");
                                for (Participant p : participants) {
                                    System.out.println(p);
                                }
                            }
                            break;

                        case 3:
                            participants = FileHandler.loadParticipants(FILE_PATH);

                            if (participants == null || participants.isEmpty()) {
                                System.out.println("No participants found. Upload CSV first.");
                            } else {

                                System.out.print("Enter desired team size: ");
                                int teamSize = sc.nextInt();
                                sc.nextLine();

                                teams = TeamBuilder.formTeams(participants, teamSize);
                                remainingPool = TeamBuilder.getRemainingParticipants();

                                System.out.println("\nTeams Formed Successfully!");

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
                                }
                            }
                            break;

                        case 4:
                            if (teams == null || teams.isEmpty()) {
                                System.out.println("Teams not formed yet. Please form teams first.");
                            } else {
                                TeamFileHandler.saveTeamsToCSV(teams, OUTPUT_PATH);
                                System.out.println("\nTeams saved to: " + OUTPUT_PATH);

                                System.out.println("Returning to main menu...");
                                organizerRunning = false; // exit organizer panel
                            }
                            break;

                        case 5:
                            organizerRunning = false;
                            break;

                        default:
                            System.out.println("Invalid option. Try again!");
                            break;
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
        }

        sc.close();
    }
}
