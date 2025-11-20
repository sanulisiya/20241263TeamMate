package service;

import model.Participant;
import java.util.*;
import java.io.*;

public class Pcreator {

    private static final String[] ALLOWED_ROLES = {"Strategist", "Attacker", "Defender", "Supporter", "Coordinator"};

    public static void createNewParticipant(String filePath) {
        Scanner sc = new Scanner(System.in);

        try { // Minimal try-catch for the whole creation flow
            // Generate unique ID
            int nextId = getNextId(filePath);
            String id = "P" + nextId;

            System.out.println("\n=== Add New Participant ===");
            System.out.println("Assigned Participant ID: " + id);

            // ---------------- Name Input ----------------
            String name;
            do {
                name = getNonEmptyInput(sc, "Enter Name: ");
                if (!service.ParticipantValidator.validateName(name)) {
                    System.out.println(" Innvalid name. Only letters and spaces allowed (2-50 characters).");
                }
            } while (!service.ParticipantValidator.validateName(name));

            // ---------------- Email Input ----------------
            String email;
            do {
                email = getNonEmptyInput(sc, "Enter Email: ");
                if (!service.ParticipantValidator.validateEmail(email)) {
                    System.out.println(" Invalid email format. Please try again.");
                }
            } while (!service.ParticipantValidator.validateEmail(email));

            // ---------------- Preferred Game ----------------
            String preferredGame;
            do {
                preferredGame = getNonEmptyInput(sc,
                        "Enter Preferred Game (Valorant, Dota, FIFA, Basketball, Badminton): ");
                if (!service.ParticipantValidator.validateGame(preferredGame)) {
                    System.out.println(" Invalid game selection. Choose from allowed games.");
                }
            } while (!service.ParticipantValidator.validateGame(preferredGame));

            // ---------------- Role Selection ----------------
            int roleSelection = getValidatedInt(sc, roleTablePrompt(), 1, 5);
            String preferredRole = ALLOWED_ROLES[roleSelection - 1];

            // ---------------- Skill Level ----------------
            int skillLevel = getValidatedInt(sc, "Enter Skill Level (1-10): ", 1, 10);

            // ---------------- Personality Survey ----------------
            System.out.println("\nNow, let's complete the 5-question Personality Survey:");
            int personalityScore = Service.Survey.conductPersonalitySurvey();
            String personalityType = Service.Survey.classifyPersonality(personalityScore);

            // Final validation check
            if (!service.ParticipantValidator.validateParticipant(name, email, skillLevel, preferredGame, preferredRole, personalityType)) {
                System.out.println(" Error: Participant data invalid. Restarting entry.");
                return;
            }

            // ---------------- Create Participant Object ----------------
            Participant p = new Participant(id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            // ---------------- Save to CSV ----------------
            try {
                service.FileHandler.saveParticipant(filePath, p);
                System.out.println("\n Participant added successfully!");
                System.out.println("Personality Type: " + personalityType + " (" + personalityScore + ")\n");
                System.out.println("\n Participant Details:");
                System.out.println(p); // Uses Participant's toString() method
            } catch (Exception e) {
                System.out.println(" Error saving participant. Check file path or permissions.");
            }

        } catch (Exception e) {
            System.out.println(" Unexpected error occurred. Please try again.");
        }
    }

    // ---------------- Helpers ----------------
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
        } catch (Exception ignored) {
        }
        return maxId + 1;
    }

    private static String getNonEmptyInput(Scanner sc, String prompt) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = sc.nextLine().trim();
            if (!input.isEmpty()) break;
            System.out.println(" Input cannot be empty. Try again.");
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
                System.out.println(" Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println(" Invalid number. Enter a numeric value.");
            }
        }
        return value;
    }

    private static String roleTablePrompt() {
        return """
                
                ┌────┬─────────────┬──────────────────────────────────────────────────────┐
                │ No │ Role        │ Description                                          │
                ├────┼─────────────┼──────────────────────────────────────────────────────┤
                │ 1  │ Strategist  │ Focuses on tactics, strategy, and game planning.     │
                │ 2  │ Attacker    │ Frontline player; aggressive and offensive style.    │
                │ 3  │ Defender    │ Protects, stabilizes, and supports team defense.     │
                │ 4  │ Supporter   │ Provides boosts, healing, and helps teammates.       │
                │ 5  │ Coordinator │ Communication leader; ensures smooth coordination.   │
                └────┴─────────────┴──────────────────────────────────────────────────────┘
                
                Enter role number (1-5): """;
    }

}