//package service;
//
//import model.PersonalityType; // Assuming you use the model.PersonalityType enum
//import java.util.concurrent.Callable; // Using Callable for returning the score
//
//// Using Callable to return the scaled score, which is often cleaner than Runnable
//public class SurveyProcessor implements Callable<Integer> {
//
//    private final int rawTotal;
//
//    // Constructor accepts the raw total score from the survey input
//    public SurveyProcessor(int rawTotal) {
//        this.rawTotal = rawTotal;
//    }
//
//    // The call() method contains the concurrent processing logic
//    @Override
//    public Integer call() throws Exception {
//        // --- Core Processing Logic ---
//
//        // Scale total to 100
//        int scaledScore = rawTotal * 4;
//
//        // Classify the personality type
//        String type = classifyPersonality(scaledScore);
//
//        // Display results (This part runs on the separate thread)
//        System.out.println("\n===== Personality Summary (Processed Concurrently) =====");
//        System.out.println("Raw Score: " + rawTotal + " (out of 25)");
//        System.out.println("Scaled Score: " + scaledScore + " (out of 100)");
//        System.out.println("Personality Type: " + type);
//        System.out.println("Description: " + getTypeDescription(type));
//        System.out.println("=======================================================");
//
//        return scaledScore;
//    }
//
//    // Helper methods moved from the original Survey class
//
//    public static String classifyPersonality(int score) {
//        // NOTE: Ensure these classifications align with your PersonalityType enum names
//        if (score >= 90) return PersonalityType.LEADER.name();
//        else if (score >= 70) return PersonalityType.BALANCED.name();
//        else if (score>=50) return PersonalityType.THINKER.name();
//        else return PersonalityType.MOTIVATOR.name();
//    }
//
//    public static String getTypeDescription(String type) {
//        return switch (type) {
//            case "LEADER" -> "Confident, decision-maker, naturally takes charge.";
//            case "BALANCED" -> "Adaptive, communicative, and team-oriented.";
//            case "THINKER" -> "Observant, analytical, and prefers planning before action.";
//            case "MOTIVATOR" -> "Boosting morale and encouraging collaboration to achieve goals.";
//            default -> "Unknown type.";
//        };
//    }
//}