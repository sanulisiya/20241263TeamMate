package service;

import java.util.Scanner;

public class Survey {

    //conduct personality Servuey
    //05.(Add new Particpnt Sequence digram)
    public static int conductPersonalitySurvey() {
        Scanner sc = new Scanner(System.in);
        int total = 0;

        System.out.println("\n===== Personality Type Survey =====");
        System.out.println("Please rate each statement from 1 (Strongly Disagree) to 5 (Strongly Agree)\n");

        String[] questions = {
                "1. I enjoy taking the lead and guiding others during group activities.  ",
                "2. I prefer analyzing situations and coming up with strategic solutions.",
                "3. I work well with others and enjoy collaborative teamwork.            ",
                "4. I am calm under pressure and can help maintain team morale.          ",
                "5. I like making quick decisions and adapting in dynamic situations.    "
        };

        for (int i = 0; i < questions.length; i++) {
            String question = questions[i];

            // Get answer with timeout using ThreadManager
            Integer answer = SurveyThreadManager.getAnswerWithTimeout(question, 30, i + 1);

            if (answer == null) {
                System.out.println("No valid answer provided, using default value 3");
                answer = 3;
            }

            total += answer;
        }

        int scaledScore = total * 4;
        String type = classifyPersonality(scaledScore);

        //Display Survey summery
        System.out.println("\n===== Personality Summary =====");
        System.out.println("Raw Score: " + total + " (out of 25)");
        System.out.println("Scaled Score: " + scaledScore + " (out of 100)");
        System.out.println("Personality Type: " + type);
        System.out.println("Description: " + getTypeDescription(type));

        return scaledScore;
    }
    //Claasify personality Type
    public static String classifyPersonality(int score) {
        if (score >= 90) return "Leader";
        else if (score >= 70) return "Balanced";
        else if (score >= 50) return "Thinker";
        else return "Motivator";
    }

    //Get personality type description
    public static String getTypeDescription(String type) {
        return switch (type) {
            case "Leader" -> "Confident, decision-maker, naturally takes charge.";
            case "Balanced" -> "Adaptive, communicative, and team-oriented.";
            case "Thinker" -> "Observant, analytical, and prefers planning before action.";
            case "Motivator" -> "Boosting morale and encouraging collaboration to achieve goals.";
            default -> "Unknown type.";
        };
    }
}