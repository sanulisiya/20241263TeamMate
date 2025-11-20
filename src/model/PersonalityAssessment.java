package model;

import java.util.Map;

public class PersonalityAssessment {
    private int totalScore;
    private String personalityType;
    private Map<String, Integer> questionScores;

    private static final String[] QUESTIONS = {
            "I enjoy taking the lead...",
            "I prefer analyzing situations...",
            // ... all 5 questions
    };

    // Domain logic methods
    public void recordAnswer(int questionNum, int score) { }
    public String determinePersonalityType() {
        return "";
    }
    public String getPersonalityDescription() {
        return "";
    }
    public static String[] getQuestions() {
        return new String[0];
    }

    public int getTotalScore() {
        return 0;
    }
}

