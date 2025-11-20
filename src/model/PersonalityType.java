/*
package model;

import java.util.Arrays;

public enum PersonalityType {

    // 90-100
    LEADER(90, "Confident, decision-maker, naturally takes charge"),

    // 70-89
    BALANCED(70, "Adaptive, communicative, team-oriented"),

    // 50-69
    THINKER(50, "Observant, analytical, prefers planning before action"),

    // Assuming 30-49 based on the previous discussion
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
     * (MOTIVATOR, with minScore=30) for any score below that range (e.g., 0-29).
     * * @param score The participant's personality score (0-100).
     * @return The corresponding PersonalityType.

public static PersonalityType classify(int score) {
    PersonalityType[] types = PersonalityType.values();

    // Sort in descending order by minScore (90, 70, 50, 30)
    Arrays.sort(types, (a, b) -> Integer.compare(b.minScore, a.minScore));

    for (PersonalityType type : types) {
        if (score >= type.minScore) {
            return type;
        }
    }

    // If the score is less than the lowest minScore (e.g., score < 30),
    // return the type with the lowest minScore (which will be MOTIVATOR if sorted correctly).
    // Since the array is sorted by minScore descending, the last element is the one with the lowest minScore.
    return types[types.length - 1];
}
}
        */