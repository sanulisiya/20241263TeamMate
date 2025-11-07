package Service;

import model.Participant;
import java.util.*;
import java.io.*;

public class Pcreator {

    public static void createNewParticipant(String filePath) {
        Scanner sc = new Scanner(System.in);
        boolean addMore = true;

        while (addMore) {
            // generate ID
            int nextId = getNextId(filePath);
            String id = "P" + nextId;

            System.out.println("\n=== Add New Participant ===");
            System.out.println("Assigned Participant ID: " + id);

            // Input genaral data
            String name = getNonEmptyInput(sc, "Enter Name: ");
            String email = getNonEmptyInput(sc, "Enter Email: ");
            String preferredGame = getNonEmptyInput(sc,
                    "Enter Preferred Game (e.g., Valorant, Dota, FIFA, Basketball, Badminton): ");

            // Role Selection Table
            System.out.println("\nChoose Your Preferred Role:");
            System.out.println("┌────┬─────────────┬──────────────────────────────────────────────┐");
            System.out.println("│ No │ Role        │ Description                                  │");
            System.out.println("├────┼─────────────┼──────────────────────────────────────────────┤");
            System.out.println("│ 1  │ Strategist  │ Focuses on tactics and planning.              │");
            System.out.println("│ 2  │ Attacker    │ Frontline player; aggressive and offensive.  │");
            System.out.println("│ 3  │ Defender    │ Protects and stabilizes the team.            │");
            System.out.println("│ 4  │ Supporter   │ Helps teammates perform better.              │");
            System.out.println("│ 5  │ Coordinator │ Communication leader; keeps team organized.  │");
            System.out.println("└────┴─────────────┴──────────────────────────────────────────────┘");

            int roleSelection = getValidatedInt(sc, "Select role number (1-5): ", 1, 5);
            String preferredRole = switch (roleSelection) {
                case 1 -> "Strategist";
                case 2 -> "Attacker";
                case 3 -> "Defender";
                case 4 -> "Supporter";
                default -> "Coordinator";
            };

            int skillLevel = getValidatedInt(sc, "Enter Skill Level (1–10): ", 1, 10);

            //  Conduct Personality Survey
            System.out.println("\nNow, let's complete the 5-question Personality Survey:");
            int personalityScore = Survey.conductPersonalitySurvey();
            String personalityType = Survey.classifyPersonality(personalityScore);

            // Create participant object
            Participant p = new Participant(id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            // Save participant to CSV
            FileHandler.saveParticipant("C:\\Users\\DELL\\Desktop\\participants_sample.csv", p);
            System.out.println("\n✅ Participant added successfully!");
            System.out.println("Personality Type: " + personalityType + " (" + personalityScore + ")\n");

            //  Ask if user wants to add another participant
            System.out.print("Do you want to add another participant? (Yes/No): ");
            String response = sc.nextLine().trim();
            if (!response.equalsIgnoreCase("Yes")) {
                addMore = false;
                System.out.println("\nReturning to main menu...");
            }
        }
    }


    private static int getNextId(String filePath) {
        int maxId = 0;
        try (Scanner fileScanner = new Scanner(new File(filePath))) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    String idStr = parts[0].replaceAll("[^0-9]", "");
                    if (!idStr.isEmpty()) {
                        int num = Integer.parseInt(idStr);
                        if (num > maxId) maxId = num;
                    }
                }
            }
        } catch (Exception e) {
            // File might be empty
        }
        return maxId + 1;
    }

    private static String getNonEmptyInput(Scanner sc, String prompt) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = sc.nextLine().trim();
            if (!input.isEmpty()) break;
            System.out.println("️ Input cannot be empty. Try again.");
        }
        return input;
    }

    private static int getValidatedInt(Scanner sc, String prompt, int min, int max) {
        int value;
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                value = Integer.parseInt(input);
                if (value >= min && value <= max) break;
                System.out.println("️ Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println(" Invalid number. Please enter a numeric value.");
            }
        }
        return value;
    }
}
