package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;
import java.util.*;
import java.io.*;

public class ParticipantCreator {

    private static final RoleType[] ALLOWED_ROLES = {
            RoleType.STRATEGIST, RoleType.ATTACKER, RoleType.DEFENDER,
            RoleType.SUPPORTER, RoleType.COORDINATOR
    };

    public static void createNewParticipant(String filePath) {
        Scanner sc = new Scanner(System.in);

        try {
            // Generate unique ID
//            int nextId = getNextId(filePath);
//            String id = "P" + nextId;

            System.out.println("\n=== Add New Participant ===");
//            System.out.println("Assigned Participant ID: " + id);



            String id;
            do{
                id = getNonEmptyInput(sc, " Enter ID :");
                if (!ParticipantValidator.validateID(id)) {
                    System.out.println(" Invalid ID. Only letter P and   numbers allowed (2-10 characters).");
                }

            }  while (!ParticipantValidator.validateID(id));

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
            do {
                email = getNonEmptyInput(sc, "Enter Email: ");
                if (!ParticipantValidator.validateEmail(email)) {
                    System.out.println(" Invalid email format. Please try again.");
                }
            } while (!ParticipantValidator.validateEmail(email));

            // ---------------- Preferred Game ----------------
            String preferredGame;
            do {
                preferredGame = getNonEmptyInput(sc,
                        "Enter Preferred Game (Valorant, Dota, FIFA, Basketball, Badminton): ");
                if (!ParticipantValidator.validateGame(preferredGame)) {
                    System.out.println(" Invalid game selection. Choose from allowed games.");
                }
            } while (!ParticipantValidator.validateGame(preferredGame));

            // ---------------- Role Selection ----------------
            int roleSelection = getValidatedInt(sc, roleTablePrompt(), 1, 5);
            RoleType preferredRole = ALLOWED_ROLES[roleSelection - 1]; // Now using GameRole enum

            // ---------------- Skill Level ----------------
            int skillLevel = getValidatedInt(sc, "Enter Skill Level (1-10): ", 1, 10);

            // ---------------- Personality Survey ----------------
            System.out.println("\nNow, let's complete the 5-question Personality Survey:");
            int personalityScore = Survey.conductPersonalitySurvey();
            PersonalityType personalityType = PersonalityType.valueOf(Survey.classifyPersonality(personalityScore).toUpperCase()); // Now using PersonalityType enum

            // Final validation check - use enum names for validation
            if (!ParticipantValidator.validateParticipant(id,name, email, skillLevel, preferredGame,
                    preferredRole.name(), personalityType.name())) {
                System.out.println(" Error: Participant data invalid. Restarting entry.");
                return;
            }

            // ---------------- Create Participant Object ----------------
            Participant p = new Participant(id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType); // Now using enums

            // ---------------- Save to CSV ----------------
            try {
                FileHandler.saveParticipant(filePath, p);
                System.out.println("\n Participant added successfully!");
                System.out.println("Personality Type: " + personalityType.name() + " (" + personalityScore + ")\n");
                System.out.println("\n Participant Details:");
                System.out.println(p); // Uses Participant's toString() method
            } catch (Exception e) {
                System.out.println(" Error saving participant. Check file path or permissions.");
                System.out.println(" Error details: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println(" Unexpected error occurred. Please try again.");
            System.out.println(" Error details: " + e.getMessage());
        }
    }

//    // ---------------- Helpers ----------------
//    private static int getNextId(String filePath) {
//        int maxId = 0;
//        try (Scanner fileScanner = new Scanner(new File(filePath))) {
//            while (fileScanner.hasNextLine()) {
//                String line = fileScanner.nextLine();
//                if (line.trim().isEmpty()) continue;
//                String[] parts = line.split(",");
//                if (parts.length > 0) {
//                    String idStr = parts[0].replaceAll("[^0-9]", "");
//                    if (!idStr.isEmpty()) {
//                        int num = Integer.parseInt(idStr);
//                        if (num > maxId) maxId = num;
//                    }
//                }
//            }
//        } catch (Exception ignored) {
//        }
//        return maxId + 1;
//    }

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
        return String.format("""
                 
                                     ┌────┬─────────────┬──────────────────────────────────────────────────────┐
                                     │ No │ Role        │ Description                                          │
                                     ├────┼─────────────┼──────────────────────────────────────────────────────┤
                                     │ 1  │ Strategist  │ Focuses on tactics and planning. Keeps the bigger pi │
                                     │ 2  │ Attacker    │ Frontline player. Good reflexes, offensive tactics,  │
                                     │ 3  │ Defender    │ Protects and supports team stability. Good under pre │
                                     │ 4  │ Supporter   │ Jack-of-all-trades. Adapts roles, ensures smooth coo │
                                     │ 5  │ Coordinator │ Communication lead. Keeps the team informed and orga │
                                     └────┴─────────────┴──────────────────────────────────────────────────────┘
                Enter role number (1-5): """,
                RoleType.STRATEGIST.getDescription(),
                RoleType.ATTACKER.getDescription(),
                RoleType.DEFENDER.getDescription(),
                RoleType.SUPPORTER.getDescription(),
                RoleType.COORDINATOR.getDescription());
    }
}