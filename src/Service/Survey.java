package Service;

import model.Participant;
import java.util.Scanner;

public class Survey {

    /**
     * Conducts a survey to collect participant info and returns a Participant object.
     */
    public static Participant conductSurvey() {
        Scanner sc = new Scanner(System.in);

        System.out.println("\n=== PARTICIPANT SURVEY ===");

        System.out.print("Enter ID: ");
        String id = sc.nextLine();

        System.out.print("Enter Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        System.out.print("Enter Preferred Game (e.g., Football, Cricket): ");
        String preferredGame = sc.nextLine();

        System.out.print("Enter Skill Level (1–10): ");
        int skillLevel = sc.nextInt();
        sc.nextLine(); // consume newline

        System.out.print("Enter Preferred Role (e.g., Attacker, Defender, Support): ");
        String preferredRole = sc.nextLine();

        System.out.print("Enter Personality Score (1–100): ");
        int personalityScore = sc.nextInt();
        sc.nextLine(); // consume newline

        System.out.print("Enter Personality Type (Leader / Thinker / Balanced): ");
        String personalityType = sc.nextLine();

        Participant participant = new Participant(
                id,
                name,
                email,
                preferredGame,
                skillLevel,
                preferredRole,
                personalityScore,
                personalityType
        );

        System.out.println("\n✅ Survey completed for " + name + "!");
        return participant;
    }

    /**
     * Conducts the survey and immediately saves the participant to CSV.
     */
    public static void conductAndSaveSurvey(String filePath) {
        Participant p = conductSurvey();
        FileHandler.saveParticipant(filePath, p);
        System.out.println("✅ Participant saved to CSV: " + filePath);
    }
}
