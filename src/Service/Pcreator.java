package Service;

import model.Participant;
import java.util.Scanner;

public class Pcreator {

    public static void createNewParticipant(String filePath) {
        Scanner sc = new Scanner(System.in);

        System.out.println("\n=== Add New Participant ===");
        System.out.print("Enter ID: ");
        String id = sc.nextLine();

        System.out.print("Enter Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        System.out.print("Enter Preferred Game: ");
        String preferredGame = sc.nextLine();

        System.out.print("Enter Skill Level (1–10): ");
        int skillLevel = sc.nextInt();
        sc.nextLine(); // consume newline

        System.out.print("Enter Preferred Role (e.g., Attacker, Defender, Support): ");
        String preferredRole = sc.nextLine();

        System.out.print("Enter Personality Score (1–100): ");
        int personalityScore = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Personality Type (Leader / Thinker / Balanced): ");
        String personalityType = sc.nextLine();

        // ✅ Create and save participant
        Participant p = new Participant(id, name, email, preferredGame, skillLevel, preferredRole, personalityScore, personalityType);
        FileHandler.saveParticipant(filePath, p);

        System.out.println("\n✅ Participant added successfully!");
    }
}
