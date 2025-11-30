package service;

import java.util.Scanner;
import java.util.concurrent.*;

public class SurveyThreadManager {
    private static final ExecutorService executor = Executors.newCachedThreadPool();


    /**
     * Gets user answer with timeout
     */
    public static Integer getAnswerWithTimeout(String question, int timeoutSeconds, int questionNumber) {
        Callable<Integer> task = () -> {
            Scanner sc = new Scanner(System.in);
            int answer = -1;
            boolean valid = false;

            while (!valid && !Thread.currentThread().isInterrupted()) {
                System.out.print(question + " → Your answer (1-5): ");
                String input = sc.nextLine().trim();

                try {
                    answer = Integer.parseInt(input);
                    if (answer >= 1 && answer <= 5) {
                        valid = true;
                    } else {
                        System.out.println("Please enter a number between 1 and 5 only.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a numeric value between 1 and 5.");
                }
            }
            return valid ? answer : null;
        };

        Future<Integer> future = executor.submit(task);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("\n Time's up! Moving to next question.");
            future.cancel(true);
            return 3; // Default neutral answer
        } catch (Exception e) {
            System.out.println("Error getting answer: " + e.getMessage());
            return 3;
        }
    }

    /**
     * Shuts down the thread manager
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}