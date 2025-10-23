package Main;

import model.Participant;
import Service.FileHandler;
import Service.Pcreator;
import Service.TeamBuilder;
import Service.TeamFileHandler;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String path = "C:\\Users\\DELL\\Desktop\\participants_sample.csv";
        String outputPath = "C:\\Users\\DELL\\Desktop\\formatted_teams.csv";
        Scanner sc = new Scanner(System.in);

        System.out.println("==== TEAMMATE COMMUNITY SYSTEM ====");
        System.out.println("1. Add new participant");
        System.out.println("2. View all participants");
        System.out.println("3. Form, display, and save balanced teams");
        System.out.print("Choose an option: ");
        int choice = sc.nextInt();
        sc.nextLine(); // consume newline

        if (choice == 1) {
            // ✅ Add new participant
            Pcreator.createNewParticipant(path);

        } else if (choice == 2) {
            // ✅ View all participants
            List<Participant> participants = FileHandler.loadParticipants(path);
            if (participants != null && !participants.isEmpty()) {
                System.out.println("\n--- Loaded Participants ---");
                for (Participant p : participants) {
                    System.out.println(p);
                }
            } else {
                System.out.println("No data found in CSV file.");
            }

        } else if (choice == 3) {
            // ✅ Form balanced teams and save them
            List<Participant> participants = FileHandler.loadParticipants(path);

            if (participants == null || participants.isEmpty()) {
                System.out.println("No participants found. Please add participants first.");
                return;
            }

            System.out.print("Enter desired team size (e.g., 5): ");
            int teamSize = sc.nextInt();
            sc.nextLine();

            List<List<Participant>> teams = TeamBuilder.formTeams(participants, teamSize);

            // ✅ Display teams
            for (int i = 0; i < teams.size(); i++) {
                System.out.println("\n=== TEAM " + (i + 1) + " ===");
                for (Participant p : teams.get(i)) {
                    System.out.println(p);
                }
            }

            // ✅ Save teams to CSV file
            TeamFileHandler.saveTeamsToCSV(teams, outputPath);

        } else {
            System.out.println("Invalid choice.");
        }
    }
}
