package service;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SurveyThreadManager {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final AtomicBoolean progressRunning = new AtomicBoolean(false);
    private static Future<?> progressFuture;
    private static final List<CompletableFuture<Void>> pendingOperations = new ArrayList<>();

    // Thread-safe scanner (one per thread)
    private static final ThreadLocal<Scanner> threadScanners = ThreadLocal.withInitial(() -> new Scanner(System.in));

    /**
     * Starts a progress indicator in a separate daemon thread
     */
    public static void startProgressIndicator(int totalQuestions) {
        if (progressRunning.compareAndSet(false, true)) {
            progressFuture = executor.submit(() -> {
                String[] spinner = new String[]{"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};
                int spinIndex = 0;
                int currentQuestion = 0;

                while (progressRunning.get() && currentQuestion < totalQuestions) {
                    System.out.printf("\r%s Progress: %d/%d questions answered",
                            spinner[spinIndex % spinner.length], currentQuestion, totalQuestions);
                    spinIndex++;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // This would typically be updated by the main thread
                    // For simplicity, we're simulating progress
                    if (spinIndex % 10 == 0) currentQuestion++;
                }
                System.out.print("\r"); // Clear progress line
            });
        }
    }

    /**
     * Stops the progress indicator
     */
    public static void stopProgressIndicator() {
        progressRunning.set(false);
        if (progressFuture != null) {
            progressFuture.cancel(true);
        }
    }

    /**
     * Gets user answer with timeout
     */
    public static Integer getAnswerWithTimeout(String question, int timeoutSeconds, int questionNumber) {
        Callable<Integer> task = () -> {
            Scanner sc = threadScanners.get();
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
            System.out.println("\nTime's up! Moving to next question.");
            future.cancel(true);
            return 3; // Default neutral answer
        } catch (Exception e) {
            System.out.println("Error getting answer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Classifies personality asynchronously
     */
    public static CompletableFuture<String> classifyPersonalityAsync(int score) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return Survey.classifyPersonality(score);
        }, executor);

        pendingOperations.add(future.thenAccept(result -> {
            // Optional: do something with the result
        }));

        return future;
    }

    /**
     * Saves question result asynchronously
     */
    public static void saveQuestionResultAsync(int questionNum, String question, int answer) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter("survey_details.log", true)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.write(String.format("[%s] Q%d: %s | Answer: %d%n",
                        timestamp, questionNum, question, answer));
            } catch (IOException e) {
                System.err.println("Failed to log question result: " + e.getMessage());
            }
        }, executor);

        pendingOperations.add(future);
    }

    /**
     * Saves final survey result asynchronously
     */
    public static void saveSurveyResultAsync(int rawScore, int scaledScore, String personalityType) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter("survey_results.log", true))) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.printf("[%s] Raw: %d, Scaled: %d, Type: %s%n",
                        timestamp, rawScore, scaledScore, personalityType);
            } catch (IOException e) {
                System.err.println("Failed to save survey result: " + e.getMessage());
            }
        }, executor);

        pendingOperations.add(future);
    }

    /**
     * Conducts multiple surveys in parallel
     */
    public static List<CompletableFuture<Integer>> conductParallelSurveys(int count) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int surveyNum = i + 1;
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                System.out.printf("\n=== Starting Survey %d ===%n", surveyNum);
                return Survey.conductPersonalitySurvey();
            }, executor);

            futures.add(future);
        }

        return futures;
    }

    /**
     * Waits for all pending operations to complete
     */
    public static void waitForCompletion() {
        CompletableFuture.allOf(
                pendingOperations.toArray(new CompletableFuture[0])
        ).join();
    }

    /**
     * Shuts down the thread manager and cleans up resources
     */
    public static void shutdown() {
        stopProgressIndicator();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Clean up thread-local resources
        threadScanners.remove();
    }

    /**
     * Gets statistics about completed operations
     */
    public static CompletableFuture<String> getSurveyStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500); // Simulate some processing
                return "Survey Statistics: " + pendingOperations.size() + " operations completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Statistics generation interrupted";
            }
        }, executor);
    }
}