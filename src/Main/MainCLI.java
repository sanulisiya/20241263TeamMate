package Main;

import Service.FileHandler;
import Service.Pcreator;
import Service.TeamBuilder;
import Service.TeamFileHandler;
import model.Participant;

import java.util.*;

public class MainCLI {

    private static String FILE_PATH = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
    private static String OUTPUT_PATH = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Participant> participants = FileHandler.loadParticipants(FILE_PATH);
        List<List<Participant>> teams = null;

        System.out.println("======================================");
        System.out.println("🎮 TEAM MATE COMMUNITY SYSTEM");
        System.out.println("======================================");

        // User Role Selection
        System.out.println("Login as:");
        System.out.println("1. Participant");
        System.out.println("2. Organizer");
        System.out.print("Enter your choice: ");

        int roleChoice;
        try {
            roleChoice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println(" Invalid input. Exiting program.");
            return;
        }

        if (roleChoice == 1) {
            // Participant Flow
            System.out.println("\n--- PARTICIPANT REGISTRATION ---");
            Pcreator.createNewParticipant(FILE_PATH);

            System.out.println("\n Registration Completed Successfully!");
            System.out.println("\nDo you want to:");
            System.out.println("1. View Assigned Team");
            System.out.println("2. Exit");
            System.out.print("Enter choice: ");
            int pChoice = sc.nextInt();
            sc.nextLine();

            if (pChoice == 1) {
                System.out.println("\n️ Teams have not been formed yet. Please check later!");
            }

            System.out.println("\n Thank you for joining!");
            System.out.println("Returning to main menu...");

        } else if (roleChoice == 2) {

            boolean running = true;
            while (running) {
                System.out.println("\n==============================");
                System.out.println("          ORGANIZER MENU      ");
                System.out.println("==============================");
                System.out.println("1. Formation of Teams");
                System.out.println("2. View All Participants");
                System.out.println("3. Save Formed Teams");
                System.out.println("4. Upload CSV File");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice;
                try {
                    choice = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("️ Please enter a number (1–5).");
                    continue;
                }

                switch (choice) {
                    case 1:
                        participants = FileHandler.loadParticipants(FILE_PATH);
                        if (participants == null || participants.isEmpty()) {
                            System.out.println(" No participants found. Upload CSV first!");
                            break;
                        }

                        System.out.print("Enter desired team size: ");
                        int teamSize;
                        try {
                            teamSize = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println(" Invalid input. Enter a number.");
                            break;
                        }

                        teams = TeamBuilder.formTeams(participants, teamSize);
                        System.out.println("\n Teams Formed Successfully!");

                        int tNum = 1;
                        for (List<Participant> team : teams) {
                            System.out.println("\n--- TEAM " + tNum++ + " ---");
                            for (Participant p : team) {
                                System.out.println(p);
                            }
                        }
                        break;

                    case 2:
                        participants = FileHandler.loadParticipants(FILE_PATH);
                        if (participants == null || participants.isEmpty()) {
                            System.out.println(" No participants found!");
                        } else {
                            System.out.println("\n=== PARTICIPANT LIST ===");
                            for (Participant p : participants) {
                                System.out.println(p);
                            }
                        }
                        break;

                    case 3:
                        if (teams == null) {
                            System.out.println("⚠ Please form teams first!");
                        } else {
                            TeamFileHandler.saveTeamsToCSV(teams, OUTPUT_PATH);
                            System.out.println(" Teams saved to: " + OUTPUT_PATH);
                        }
                        break;

                    case 4:
                        System.out.print("Enter new CSV file path: ");
                        FILE_PATH = sc.nextLine();
                        participants = FileHandler.loadParticipants(FILE_PATH);

                        if (participants != null && !participants.isEmpty()) {
                            System.out.println(" CSV Loaded! Total Participants: " + participants.size());
                        } else {
                            System.out.println(" Failed to load CSV. Check file path!");
                        }
                        break;

                    case 5:
                        System.out.println("\n Organizer Logged Out!");
                        running = false;
                        break;

                    default:
                        System.out.println(" Invalid choice! Try again.");
                }
            }

        } else {
            System.out.println(" Invalid role choice. Exiting...");
        }

        sc.close();
    }
}
