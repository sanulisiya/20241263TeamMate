package model;

public enum PersonalityType {
    LEADER,
    THINKER,
    BALANCED,
    MOTIVATOR;

    /**
     * Safely converts any string (even with descriptions inside parentheses)
     * into a valid PersonalityType enum.
     *
     * Examples:
     *  "LEADER (Confident)"  → LEADER
     *  "thinker(observant)"  → THINKER
     *  "Balanced"            → BALANCED
     *  "MOTIVATOR (Positive)" → MOTIVATOR
     */
    public static PersonalityType fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return PersonalityType.BALANCED; // default
        }

        // Remove anything inside parentheses
        String cleaned = typeString.split("\\(")[0].trim().toUpperCase();

        try {
            return PersonalityType.valueOf(cleaned);
        } catch (Exception e) {
            System.err.println("Invalid personality type: " + typeString +
                    " (cleaned: " + cleaned + "), defaulting to BALANCED");
            return PersonalityType.BALANCED;
        }
    }
}
