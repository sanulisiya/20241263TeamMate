package utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LoggerService {

    private static final String LOG_FILE = getLogFilePath();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LogLevel currentLogLevel = LogLevel.INFO;

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    // Private constructor to prevent instantiation
    private LoggerService() {}

    /**
     * Get proper log file path
     */
    private static String getLogFilePath() {
        // Store in project directory (current working directory)
        String projectDir = System.getProperty("user.dir");
        return projectDir + File.separator + "teammate_system.log";
    }

    /**
     * Ensure log directory exists - FIXED VERSION
     */
    private static void ensureLogDirectoryExists() {
        try {
            File logFile = new File(LOG_FILE);
            File parentDir = logFile.getParentFile();

            // Only create directories if parentDir is not null (file has a parent path)
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    System.out.println("Created log directory: " + parentDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set the current logging level
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
        info("Log level changed to: " + level);
    }

    /**
     * Get current log level
     */
    public static LogLevel getCurrentLogLevel() {
        return currentLogLevel;
    }

    /**
     * Log debug messages
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public static void debug(String message, String className, String methodName) {
        log(LogLevel.DEBUG, formatMessage(message, className, methodName), null);
    }

    /**
     * Log info messages
     */
    public static void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public static void info(String message, String className, String methodName) {
        log(LogLevel.INFO, formatMessage(message, className, methodName), null);
    }

    /**
     * Log warning messages
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public static void warn(String message, String className, String methodName) {
        log(LogLevel.WARN, formatMessage(message, className, methodName), null);
    }

    public static void warn(String message, Exception e) {
        log(LogLevel.WARN, message, e);
    }

    public static void warn(String message, Exception e, String className, String methodName) {
        log(LogLevel.WARN, formatMessage(message, className, methodName), e);
    }

    /**
     * Log error messages
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public static void error(String message, String className, String methodName) {
        log(LogLevel.ERROR, formatMessage(message, className, methodName), null);
    }

    public static void error(String message, Exception e) {
        log(LogLevel.ERROR, message, e);
    }

    public static void error(String message, Exception e, String className, String methodName) {
        log(LogLevel.ERROR, formatMessage(message, className, methodName), e);
    }

    /**
     * Main logging method - FIXED VERSION
     */
    private static void log(LogLevel level, String message, Exception e) {
        // Check if we should log this message based on current log level
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // Print to console
        if (level == LogLevel.ERROR) {
            System.err.println(logEntry);
        } else {
            System.out.println(logEntry);
        }

        // Print stack trace to console if exception provided
        if (e != null) {
            if (level == LogLevel.ERROR) {
                System.err.println("Exception details:");
                e.printStackTrace(System.err);
            } else {
                System.out.println("Exception details:");
                e.printStackTrace(System.out);
            }
        }

        // Write to file with better error handling
        boolean writeSuccess = writeToFile(logEntry, e);
        if (!writeSuccess && level == LogLevel.ERROR) {
            System.err.println("CRITICAL: Failed to write log to file: " + logEntry);
        }
    }

    /**
     * Write log entry to file - FIXED VERSION
     */
    private static synchronized boolean writeToFile(String logEntry, Exception e) {
        try {
            ensureLogDirectoryExists(); // Ensure directory exists

            try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
                writer.println(logEntry);

                // Write exception stack trace if provided
                if (e != null) {
                    writer.println("Exception Details:");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    writer.println(sw.toString());
                    writer.println(); // Add empty line for separation
                }

                writer.flush();
                return true;
            }

        } catch (IOException ioException) {
            // Use System.err since logging might be broken
            System.err.println("Failed to write to log file '" + LOG_FILE + "': " + ioException.getMessage());
            ioException.printStackTrace();
            return false;
        } catch (Exception ex) {
            System.err.println("Unexpected error while writing to log file: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Format message with class and method context
     */
    private static String formatMessage(String message, String className, String methodName) {
        if (className != null && methodName != null) {
            return String.format("[%s.%s] %s", className, methodName, message);
        } else if (className != null) {
            return String.format("[%s] %s", className, message);
        }
        return message;
    }

    /**
     * Log participant-related activities
     */
    public static void logParticipantAction(String action, String participantId, String details) {
        info(String.format("PARTICIPANT %s: %s - %s", participantId, action, details),
                "ParticipantService", "logParticipantAction");
    }

    /**
     * Log team formation activities
     */
    public static void logTeamFormation(String action, int teamCount, int participantCount) {
        info(String.format("TEAM FORMATION: %s - Teams: %d, Participants: %d",
                action, teamCount, participantCount), "TeamBuilder", "logTeamFormation");
    }

    /**
     * Enhanced team formation logging with complete teams and remaining participants
     */
    public static void logTeamFormation(String action, int teamCount, int participantCount,
                                        int completeTeams, int remainingParticipants) {
        info(String.format("TEAM FORMATION: %s - Total Teams: %d, Participants: %d, Complete Teams: %d, Remaining: %d",
                        action, teamCount, participantCount, completeTeams, remainingParticipants),
                "TeamBuilder", "logTeamFormation");
    }

    /**
     * Detailed team formation logging with map of details
     */
    public static void logTeamFormation(String action, Map<String, Object> formationDetails) {
        String details = formationDetails.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        info(String.format("TEAM FORMATION: %s - %s", action, details),
                "TeamBuilder", "logTeamFormation");
    }

    /**
     * Log file operations
     */
    public static void logFileOperation(String operation, String filePath, String result) {
        info(String.format("FILE %s: %s - %s", operation, filePath, result),
                "FileHandler", "logFileOperation");
    }

    /**
     * Log user authentication
     */
    public static void logAuthentication(String userType, String userId, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        info(String.format("AUTH %s: User %s - %s", userType, userId, status),
                "MainCLI", "logAuthentication");
    }

    /**
     * Clear log file (useful for testing)
     */
    public static void clearLog() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, false))) {
            writer.print("");
            System.out.println("Log file cleared successfully");
        } catch (IOException e) {
            System.err.println("Failed to clear log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test the logger functionality
     */
    public static void testLogger() {
        System.out.println("=== Logger Service Test ===");
        System.out.println("Log file path: " + LOG_FILE);
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        // Test different log levels
        debug("This is a debug message");
        info("This is an info message");
        warn("This is a warning message");
        error("This is an error message");

        // Test with exception
        try {
            throw new RuntimeException("Test exception for logging");
        } catch (RuntimeException e) {
            error("This is an error with exception", e);
        }

        // Test with class and method context
        info("Testing class/method context", "LoggerService", "testLogger");

        System.out.println("=== Logger Test Completed ===");
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        testLogger();
        System.out.println("\nCheck the log file at: " + getLogFilePath());
    }
}