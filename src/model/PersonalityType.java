package model;

import java.util.Arrays;

public enum PersonalityType {
    LEADER(90, "Confident, decision-maker, naturally takes charge"),
    BALANCED(70, "Adaptive, communicative, team-oriented"),
    THINKER(50, "Observant, analytical, prefers planning before action"),
    MOTIVATOR(0, "Encourages teammates, focuses on morale and positive outlook");

    private final int minScore;
    private final String description;

    PersonalityType(int minScore, String description) {
        this.minScore = minScore;
        this.description = description;
    }

    public int getMinScore() {
        return minScore;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Static method to determine the PersonalityType based on a score.
     * This method will return the type corresponding to the lowest range
     * (MOTIVATOR, with minScore=0) for any score below that range.
     * @param score The participant's personality score (0-100).
     * @return The corresponding PersonalityType.
     */
    public static PersonalityType classify(int score) {
        // Validate input
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Personality score must be between 0 and 100");
        }

        PersonalityType[] types = PersonalityType.values();

        // Sort in descending order by minScore (90, 70, 50, 0)
        Arrays.sort(types, (a, b) -> Integer.compare(b.minScore, a.minScore));

        for (PersonalityType type : types) {
            if (score >= type.minScore) {
                return type;
            }
        }

        // This should never be reached due to MOTIVATOR having minScore=0
        return MOTIVATOR;
    }

    /**
     * Helper method to convert string to PersonalityType (case-insensitive)
     * @param typeString The string representation of the personality type
     * @return The corresponding PersonalityType enum
     * @throws IllegalArgumentException if the string doesn't match any type
     */
    public static PersonalityType fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            throw new IllegalArgumentException("Personality type string cannot be null or empty");
        }

        try {
            return PersonalityType.valueOf(typeString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid personality type: " + typeString +
                    ". Valid values are: " + Arrays.toString(PersonalityType.values()));
        }
    }

    /**
     * Check if a string represents a valid personality type
     * @param typeString The string to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return false;
        }

        try {
            PersonalityType.valueOf(typeString.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}