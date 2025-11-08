package Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ParticipantValidator {

    //  Allowed games (canonical names)
    private static final List<String> ALLOWED_GAMES = Arrays.asList(
            "Valorant", "Dota", "FIFA", "Basketball", "Badminton", "Chess"
    );

    //  Allowed personality types
    private static final List<String> ALLOWED_PERSONALITIES = Arrays.asList(
            "Leader", "Thinker", "Balanced"
    );

    //  Email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    //  Name regex: letters and spaces, 2–50 chars
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-z ]{2,50}$"
    );

    //  Validate name
    public static boolean validateName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    //  Validate email
    public static boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    //  Validate skill level (1–10)
    public static boolean validateSkillLevel(int skill) {
        return skill >= 1 && skill <= 10;
    }

    //  Validate game (case-insensitive)
    public static boolean validateGame(String game) {
        if (game == null) return false;
        return ALLOWED_GAMES.stream()
                .anyMatch(g -> g.equalsIgnoreCase(game.trim()));
    }

    //  Get normalized game name (for consistent storage/display)
    public static String getNormalizedGame(String game) {
        if (game == null) return null;
        for (String g : ALLOWED_GAMES) {
            if (g.equalsIgnoreCase(game.trim())) {
                return g; // return canonical name (e.g., "Valorant")
            }
        }
        return null;
    }

    // Validate personality type (case-insensitive)
    public static boolean validatePersonalityType(String type) {
        if (type == null) return false;
        return ALLOWED_PERSONALITIES.stream()
                .anyMatch(p -> p.equalsIgnoreCase(type.trim()));
    }

    // Validate preferred role (non-empty)
    public static boolean validateRole(String role) {
        return role != null && !role.trim().isEmpty();
    }

    // Full participant validation
    public static boolean validateParticipant(String name, String email, int skill, String game, String role, String personalityType) {
        return validateName(name) &&
                validateEmail(email) &&
                validateSkillLevel(skill) &&
                validateGame(game) &&
                validateRole(role) &&
                validatePersonalityType(personalityType);
    }
}
