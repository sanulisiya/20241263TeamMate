package Main;

import model.Participant;
import Service.FileHandler;
import Service.Pcreator;
import Service.TeamBuilder;
import Service.TeamFileHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        String path = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
        String outputPath = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";

        List<Participant> participants = null;
        List<List<Participant>> teams = null;
        List<Participant> remainingPool = new ArrayList<>();

        System.out.println("==== TEAMMATE COMMUNITY SYSTEM ====");
        System.out.println("Login as:");
        System.out.println("1. Participant");
        System.out.println("2. Organizer");
        System.out.print("Select option : ");
        int loginChoice = sc.nextInt();
        sc.nextLine();

        if (loginChoice == 1) {
            // Participant Registration
            Pcreator.createNewParticipant(path);



        } else if (loginChoice == 2) {

            boolean running = true;

            while (running) {
                System.out.println("\n--- ORGANIZER PANEL ---");
                System.out.println("1. Formation of Teams");
                System.out.println("2. View All Participants");
                System.out.println("3. Save Formed Teams");
                System.out.println("4. Upload CSV");
                System.out.println("5. Exit");
                System.out.print("Select option: ");

                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        participants = FileHandler.loadParticipants(path);
                        if (participants == null || participants.isEmpty()) {
                            System.out.println("No participants found. Upload CSV first.");
                        } else {
                            System.out.print("Enter desired team size (e.g. 5): ");
                            int teamSize = sc.nextInt();
                            sc.nextLine();
// Form teams using the improved matching algorithm
                            teams = TeamBuilder.formTeams(participants, teamSize);

// Get the remaining participants (extra leaders or leftover players)
                            remainingPool = TeamBuilder.getRemainingParticipants();


                            System.out.println("\n✅ Teams Formed Successfully!");

                            // Display formed teams
                            for (int i = 0; i < teams.size(); i++) {
                                System.out.println("\n======= TEAM " + (i + 1) + " =======");
                                for (Participant p : teams.get(i)) {
                                    System.out.println(p);
                                }
                            }

                            // Display unassigned participants (remaining pool)
                            if (!remainingPool.isEmpty()) {
                                System.out.println("\n Remaining Unassigned Participants (Extra Leaders/Players):");
                                for (Participant p : remainingPool) {
                                    System.out.println(p);
                                }
                            } else {
                                System.out.println("\nAll participants successfully assigned to teams!");
                            }
                        }
                        break;

                    case 2:
                        participants = FileHandler.loadParticipants(path);
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
                        if (teams == null || teams.isEmpty()) {
                            System.out.println("Teams not formed yet. Please form teams first.");
                        } else {
                            TeamFileHandler.saveTeamsToCSV(teams, outputPath);
                            System.out.println("Teams saved to: " + outputPath);
                        }
                        break;

                    case 4:
                        System.out.print("\nEnter CSV File Path: ");
                        path = sc.nextLine();
                        participants = FileHandler.loadParticipants(path);

                        if (participants != null && !participants.isEmpty()) {
                            System.out.println("CSV Uploaded Successfully! Total Participants: " + participants.size());
                        } else {
                            System.out.println("CSV Upload Failed. Check file path!");
                        }
                        break;

                    case 5:
                        System.out.println("Organizer Logged Out Successfully!");
                        running = false;
                        break;

                    default:
                        System.out.println("Invalid Option. Try again!");
                        break;
                }
            }

        } else {
            System.out.println("Invalid Login Option!");
        }

        sc.close();
    }
}
