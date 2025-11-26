package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;
import utility.LoggerService;

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

    public static void createNewParticipant(String newCSVPath) {
        FileHandler.ensureCSVExists(newCSVPath);

        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("\n=== Add New Participant ===");

            // ---------------- ID Input ----------------
            String id;
            do {
                id = getNonEmptyInput(sc, "Enter Participant ID (e.g., P001, P10, P100): ");
                if (!ParticipantValidator.validateId(id)) {
                    System.out.println("Invalid ID format. Please use format like P1, P10, P100 (no leading zeros).");
                } else if (isIdAlreadyExists(newCSVPath, id)) {
                    System.out.println("This ID already exists. Please use a different ID.");
                }
            } while (!ParticipantValidator.validateId(id) || isIdAlreadyExists(newCSVPath, id));

            // ---------------- Name Input ----------------
            String name;
            do {
                name = getNonEmptyInput(sc, "Enter Name: ");
                if (!ParticipantValidator.validateName(name)) {
                    System.out.println("Invalid name. Only letters and spaces allowed (2-50 characters).");
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
                        "Enter Preferred Game (Valorant, Dota, DOTA 2, FIFA, Basketball, Badminton, Chess, CS:GO): ");
                if (!ParticipantValidator.validateGame(preferredGame)) {
                    System.out.println("Invalid game selection. Please choose from the allowed games.");
                }
            } while (!ParticipantValidator.validateGame(preferredGame));

            // Normalize game name
            preferredGame = ParticipantValidator.getNormalizedGame(preferredGame);

            // ---------------- Role Selection ----------------
            int roleSelection = getValidatedInt(sc, roleTablePrompt(), 1, 5);
            RoleType preferredRole = ALLOWED_ROLES[roleSelection - 1];

            // ---------------- Skill Level ----------------
            int skillLevel = getValidatedInt(sc, "Enter Skill Level (1-10): ", 1, 10);

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
            FileHandler.saveParticipant(newCSVPath, p);

            // ---------------- SINGLE LOGGER CALL ----------------
            LoggerService.logParticipantAction("CREATED", id,
                    "Name: " + name + ", Game: " + preferredGame + ", Role: " + preferredRole);

            System.out.println("\nParticipant added successfully!");
            System.out.println("Personality Type: " + personalityType + " (" + personalityScore + ")");
            System.out.println("\nParticipant Details:");
            System.out.println(p);

        } catch (Exception e) {
            System.out.println("Unexpected error occurred.");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------- Helper Methods ----------------
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
                System.out.println("Invalid number. Please enter a numeric value.");
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
                Enter role number (1-5):\s""";
    }

    // ---------------- ID Validation Helper ----------------
    private static boolean isIdAlreadyExists(String csvPath, String id) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;
            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > 0 && fields[0].equalsIgnoreCase(id.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not check existing IDs.");
        }
        return false;
    }
}