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

    //Create new participant method - returns Participant object
    public static Participant createNewParticipant() {
        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("\n=== REGISTER NEW PARTICIPANT ===");

            // ---------------- ID Input ----------------
            String id;
            boolean idExists = false;
            int invalidAttempts = 0;
            do {
                System.out.print("Enter Participant ID (e.g., P001, P10, P100) o: ");
                id = sc.nextLine().trim();

                // Validate ID format
                if (!ParticipantValidator.validateId(id)) {
                    invalidAttempts++;
                    System.out.println("Invalid ID format. Please use format like P1, P10, P100.");

                    // After first invalid input, ask if they want to retry or exit
                    if (invalidAttempts >= 1) {
                        System.out.print("Would you like to [1] Try again or [2] Cancel registration? ");
                        String choice = sc.nextLine().trim();
                        if (choice.equals("2")) {
                            System.out.println("Registration cancelled.");
                            return null;
                        }
                        // If choice is 1, continue with the loop
                    }
                    continue;
                }

                // Check if ID already exists
                String finalId = id;
                idExists = CSVMerger.getNewParticipants().stream()
                        .anyMatch(p -> p.getId().equalsIgnoreCase(finalId));

                if (idExists) {
                    invalidAttempts++;
                    System.out.println("Participant ID '" + id + "' already exists.");

                    // After first duplicate, ask for exit option
                    if (invalidAttempts >= 1) {
                        System.out.print("Would you like to [1] Try a different ID or [2] Cancel? ");
                        String choice = sc.nextLine().trim();
                        if (choice.equals("2")) {
                            System.out.println("Registration cancelled.");
                            return null;
                        }
                    }
                }
            } while (idExists || !ParticipantValidator.validateId(id));
            // ---------------- Name Input ----------------
            String name;
            do {
                name = getNonEmptyInput(sc, "Enter Name: ");
                if (!ParticipantValidator.validateName(name)) {
                    System.out.println(" Invalid name. Only letters and spaces allowed (2-50 characters).");
                }
            } while (!ParticipantValidator.validateName(name));

            // ---------------- Email Input ----------------
            String email;
            boolean emailExists;
            do {
                email = getNonEmptyInput(sc, "Enter Email: ");
                if (!ParticipantValidator.validateEmail(email)) {
                    System.out.println("Invalid email format.");
                    emailExists = true;
                    continue;
                }

                // Check if email already exists in merge pool
                String finalEmail = email;
                emailExists = CSVMerger.getNewParticipants().stream()
                        .anyMatch(p -> p.getEmail().equalsIgnoreCase(finalEmail));

                if (emailExists) {
                    System.out.println("Email '" + email + "' already exists in the merge pool. Please use a different email.");
                }
            } while (emailExists || !ParticipantValidator.validateEmail(email));

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
                return null;
            }

            // ---------------- Create Object ----------------
            Participant p = new Participant(
                    id, name, email, preferredGame,
                    skillLevel, preferredRole, personalityScore, personalityType
            );

            // ---------------- LOG ----------------
            LoggerService.getInstance().info("Participant CREATED - ID: " + id +
                    ", Name: " + name +
                    ", Game: " + preferredGame +
                    ", Role: " + preferredRole);

            System.out.println("\n Participant created successfully!");
            System.out.println(" Personality Type: " + personalityType + " (" + personalityScore + ")");
            System.out.println("\n Participant Details:");
            System.out.println(p);

            return p;

        } catch (Exception e) {
            System.out.println("Unexpected error occurred during participant creation.");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
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
}