package Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ParticipantValidator {

    // Allowed games
    private static final List<String> ALLOWED_GAMES = Arrays.asList(
            "Valorant", "Dota", "FIFA", "Basketball", "Badminton", "Chess"
    );

    // Allowed personality types
    private static final List<String> ALLOWED_PERSONALITIES = Arrays.asList(
            "Leader", "Thinker", "Balanced"
    );

    // Email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    // Name regex: letters and spaces, 2-50 chars
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-z ]{2,50}$"
    );

    // Validate name
    public static boolean validateName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    // Validate email
    public static boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // Validate skill level
    public static boolean validateSkillLevel(int skill) {
        return skill >= 1 && skill <= 10;
    }

    // Validate preferred game
    public static boolean validateGame(String game) {
        return game != null && ALLOWED_GAMES.contains(game);
    }

    // Validate personality type
    public static boolean validatePersonalityType(String type) {
        return type != null && ALLOWED_PERSONALITIES.contains(type);
    }

    // Validate preferred role (non-empty)
    public static boolean validateRole(String role) {
        return role != null && !role.trim().isEmpty();
    }

    // Full validation for participant
    public static boolean validateParticipant(String name, String email, int skill, String game, String role, String personalityType) {
        return validateName(name) &&
                validateEmail(email) &&
                validateSkillLevel(skill) &&
                validateGame(game) &&
                validateRole(role) &&
                validatePersonalityType(personalityType);
    }
}
