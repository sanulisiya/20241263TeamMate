package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;

import java.util.*;
import java.io.*;

public class ParticipantCreator {

    private static final RoleType[] ALLOWED_ROLES = {
            RoleType.STRATEGIST,
            RoleType.ATTACKER,
            RoleType.DEFENDER,
            RoleType.SUPPORTER,
            RoleType.COORDINATOR
    };

    public static void createNewParticipant(String filePath) {

        Scanner sc = new Scanner(System.in);

        try {
            // ---------------- AUTO-GENERATED ID ----------------
            int nextId = getNextId(filePath);
            String id = "P" + nextId;

            System.out.println("\n=== Add New Participant ===");
            System.out.println("Assigned Participant ID: " + id);

            // ---------------- Name Input ----------------
            String name;
            do {
                name = getNonEmptyInput(sc, "Enter Name: ");
                if (!ParticipantValidator.validateName(name)) {
                    System.out.println("Invalid name. Only letters and spaces allowed (2–50 characters).");
                }
            } while (!ParticipantValidator.validateName(name));

            // ---------------- Email Input ----------------
            String email;
            do {
                email = getNonEmptyInput(sc, "Enter Email: ");
                if (!ParticipantValidator.validateEmail(email)) {
                    System.out.println("Invalid email format.");
                }
            } while (!ParticipantValidator.validateEmail(email));

            // ---------------- Preferred Game ----------------
            String preferredGame;
            do {
                preferredGame = getNonEmptyInput(sc,
                        "Enter Preferred Game (Valorant, Dota, FIFA, Basketball, Badminton): ");
                if (!ParticipantValidator.validateGame(preferredGame)) {
                    System.out.println("Invalid game selection.");
                }
            } while (!ParticipantValidator.validateGame(preferredGame));

            // ---------------- Role Selection ----------------
            int roleSelection = getValidatedInt(sc, roleTablePrompt(), 1, 5);
            RoleType preferredRole = ALLOWED_ROLES[roleSelection - 1];

            // ---------------- Skill Level ----------------
            int skillLevel = getValidatedInt(sc, "Enter Skill Level (1–10): ", 1, 10);

            // ---------------- Personality Survey ----------------
            System.out.println("\nNow, let's complete the 5-question Personality Survey:");
            int personalityScore = Survey.conductPersonalitySurvey();
            PersonalityType personalityType =
                    PersonalityType.valueOf(Survey.classifyPersonality(personalityScore).toUpperCase());

            // ---------------- Final validation ----------------
            if (!ParticipantValidator.validateParticipant(
                    id, name, email, skillLevel,
                    preferredGame, preferredRole.name(), personalityType.name()
            )) {
                System.out.println("Error: participant data invalid.");
                return;
            }

            // ---------------- Create Object ----------------
            Participant p = new Participant(
                    id, name, email, preferredGame,
                    skillLevel, preferredRole, personalityScore, personalityType
            );

            // ---------------- Save to CSV ----------------
            FileHandler.saveParticipant(filePath, p);

            System.out.println("\nParticipant added successfully!");
            System.out.println("Personality Type: " + personalityType + " (" + personalityScore + ")");
            System.out.println("\nParticipant Details:");
            System.out.println(p);

        } catch (Exception e) {
            System.out.println("Unexpected error occurred.");
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------------- Helpers ----------------

    private static int getNextId(String filePath) {
        int maxId = 0;

        try (Scanner fileScanner = new Scanner(new File(filePath))) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                String idStr = parts[0].replaceAll("[^0-9]", "");

                if (!idStr.isEmpty()) {
                    int num = Integer.parseInt(idStr);
                    maxId = Math.max(maxId, num);
                }
            }
        } catch (Exception ignored) {}

        return maxId + 1;
    }

    private static String getNonEmptyInput(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("Input cannot be empty.");
        }
    }

    private static int getValidatedInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                if (value >= min && value <= max) return value;
                System.out.println("Value must be between " + min + " and " + max + ".");
            } catch (Exception e) {
                System.out.println("Invalid number.");
            }
        }
    }

    private static String roleTablePrompt() {
        return """
                ┌────┬─────────────┬─────────────────────────────────────────┐
                │ No │ Role        │ Description                             │
                ├────┼─────────────┼─────────────────────────────────────────┤
                │ 1  │ Strategist  │ Focuses on tactics and planning.        │
                │ 2  │ Attacker    │ Frontline offensive player.             │
                │ 3  │ Defender    │ Protects and stabilizes the team.       │
                │ 4  │ Supporter   │ Flexible, helps coordinate actions.     │
                │ 5  │ Coordinator │ Communication leader.                   │
                └────┴─────────────┴─────────────────────────────────────────┘
                Enter role number (1–5): 
                """;
    }
}
