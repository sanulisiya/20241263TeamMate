package service;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Survey {

    public static int conductPersonalitySurvey() {
        Scanner sc = new Scanner(System.in);
        int total = 0;

        System.out.println("\n===== Personality Type Survey =====");
        System.out.println("Please rate each statement from 1 (Strongly Disagree) to 5 (Strongly Agree)\n");

        String[] questions = {
                "1. I enjoy taking the lead and guiding others during group activities.",
                "2. I prefer analyzing situations and coming up with strategic solutions.",
                "3. I work well with others and enjoy collaborative teamwork.",
                "4. I am calm under pressure and can help maintain team morale.",
                "5. I like making quick decisions and adapting in dynamic situations."
        };

        // Start progress indicator in background
        SurveyThreadManager.startProgressIndicator(questions.length);

        for (int i = 0; i < questions.length; i++) {
            String question = questions[i];

            // Get answer with timeout capability
            Integer answer = SurveyThreadManager.getAnswerWithTimeout(question, 30, i + 1);

            if (answer == null) {
                System.out.println("No valid answer provided, using default value 3");
                answer = 3;
            }

            total += answer;

            // Save result asynchronously after each question
            SurveyThreadManager.saveQuestionResultAsync(i + 1, question, answer);
        }

        // Stop progress indicator
        SurveyThreadManager.stopProgressIndicator();

        int scaledScore = total * 4;
        String type = classifyPersonality(scaledScore);

        // Get classification asynchronously
        CompletableFuture<String> asyncType = SurveyThreadManager.classifyPersonalityAsync(scaledScore);

        try {
            type = asyncType.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Classification timed out, using synchronous method");
            type = classifyPersonality(scaledScore);
        }

        System.out.println("\n===== Personality Summary =====");
        System.out.println("Raw Score: " + total + " (out of 25)");
        System.out.println("Scaled Score: " + scaledScore + " (out of 100)");
        System.out.println("Personality Type: " + type);
        System.out.println("Description: " + getTypeDescription(type));

        // Save final result asynchronously
        SurveyThreadManager.saveSurveyResultAsync(total, scaledScore, type);

        return scaledScore;
    }

    public static String classifyPersonality(int score) {
        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (score >= 90) return "Leader";
        else if (score >= 70) return "Balanced";
        else if (score >= 50) return "Thinker";
        else return "Motivator";
    }

    public static String getTypeDescription(String type) {
        return switch (type) {
            case "Leader" -> "Confident, decision-maker, naturally takes charge.";
            case "Balanced" -> "Adaptive, communicative, and team-oriented.";
            case "Thinker" -> "Observant, analytical, and prefers planning before action.";
            case "Motivator" -> "Boosting morale and encouraging collaboration to achieve goals.";
            default -> "Unknown type.";
        };
    }

    // Method for batch processing multiple surveys
    public static CompletableFuture<Integer> conductSurveyAsync() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n--- Starting async survey ---");
            return conductPersonalitySurvey();
        });
    }
}