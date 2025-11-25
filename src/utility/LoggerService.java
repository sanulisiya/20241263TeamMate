package utility;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {

    private static final String LOG_FILE = "teammate_system.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LogLevel currentLogLevel = LogLevel.INFO;

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    // Private constructor to prevent instantiation
    private LoggerService() {}

    /**
     * Set the current logging level
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
        info("Log level changed to: " + level);
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
     * Main logging method
     */
    private static void log(LogLevel level, String message, Exception e) {
        // Check if we should log this message based on current log level
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        // Print to console
        System.out.println(logEntry);

        // Print stack trace if exception provided
        if (e != null) {
            e.printStackTrace();
        }

        // Write to file
        writeToFile(logEntry, e);
    }

    /**
     * Write log entry to file
     */
    private static synchronized void writeToFile(String logEntry, Exception e) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logEntry);

            // Write exception stack trace if provided
            if (e != null) {
                e.printStackTrace(writer);
            }

            writer.flush();
        } catch (IOException ioException) {
            System.err.println("Failed to write to log file: " + ioException.getMessage());
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
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE))) {
            writer.print("");
            info("Log file cleared");
        } catch (IOException e) {
            error("Failed to clear log file", e);
        }
    }

    /**
     * Get current log file path
     */
    public static String getLogFilePath() {
        return LOG_FILE;
    }
}