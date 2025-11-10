package Service;

import java.util.Scanner;

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

        for (String q : questions) {
            int answer = -1;
            boolean valid = false;

            while (!valid) {
                System.out.print(q + " → Your answer (1–5): ");
                String input = sc.nextLine().trim();

                try {
                    answer = Integer.parseInt(input);

                    if (answer >= 1 && answer <= 5) {
                        valid = true;
                    } else {
                        System.out.println(" Please enter a number between 1 and 5 only.");
                    }

                } catch (NumberFormatException e) {
                    System.out.println(" Invalid input. Please enter a numeric value between 1 and 5.");
                }
            }

            total += answer;
        }

        int scaledScore = total * 4;  // Scale total to 100
        String type = classifyPersonality(scaledScore);

        System.out.println("\n===== Personality Summary =====");
        System.out.println("Raw Score: " + total + " (out of 25)");
        System.out.println("Scaled Score: " + scaledScore + " (out of 100)");
        System.out.println("Personality Type: " + type);
        System.out.println("Description: " + getTypeDescription(type));

        return scaledScore;
    }

    public static String classifyPersonality(int score) {
        if (score >= 90) return "Leader";
        else if (score >= 70) return "Balanced";
        else if (score>=50) return "Thinker";
        else return "Motivator";
    }

    public static String getTypeDescription(String type) {
        return switch (type) {
            case "Leader" -> "Confident, decision-maker, naturally takes charge.";
            case "Balanced" -> "Adaptive, communicative, and team-oriented.";
            case "Thinker" -> "Observant, analytical, and prefers planning before action.";
            case "Motivator" -> "boosting morale and encouraging collaboration to achieve goals.";
            default -> "Unknown type.";
        };
    }
}
