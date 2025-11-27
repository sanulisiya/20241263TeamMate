package utility;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {

    private static LoggerService instance;     // Singleton instance
    private static final Object lock = new Object();

    private final String logFile;
    private final LogLevel currentLogLevel = LogLevel.INFO;
    private final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ----------- PRIVATE CONSTRUCTOR (Singleton Requirement) -------------
    private LoggerService() {
        this.logFile = getLogFilePath();
        ensureLogDirectoryExists();
    }

    // ----------- GET INSTANCE (Thread-Safe Singleton) -------------
    public static LoggerService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new LoggerService();
                }
            }
        }
        return instance;
    }

    // ----------- HELPERS -------------

    private static String getLogFilePath() {
        String projectDir = System.getProperty("user.dir");
        return projectDir + File.separator + "teammate_system.log";
    }

    private void ensureLogDirectoryExists() {
        try {
            File logFile = new File(this.logFile);
            File parentDir = logFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    // ----------- PUBLIC LOG METHODS -------------

    public void debug(String msg) {
        log(LogLevel.DEBUG, msg, null);
    }

    public void info(String msg) {
        log(LogLevel.INFO, msg, null);
    }

    public void warn(String msg) {
        log(LogLevel.WARN, msg, null);
    }

    public void warn(String msg, Exception e) {
        log(LogLevel.WARN, msg, e);
    }

    public void error(String msg) {
        log(LogLevel.ERROR, msg, null);
    }

    public void error(String msg, Exception e) {
        log(LogLevel.ERROR, msg, e);
    }

    // ----------- MAIN LOG FUNCTION -------------

    private void log(LogLevel level, String message, Exception e) {
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        if (!writeToFile(logEntry, e)) {
            System.err.println("CRITICAL LOGGER FAILURE: Could not write to log file!");
        }
    }

    // ----------- FILE WRITING -------------

    private synchronized boolean writeToFile(String logEntry, Exception e) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logEntry);

            if (e != null) {
                writer.println("Exception Details:");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                writer.println(sw);
                writer.println();
            }

            return true;

        } catch (IOException ioException) {
            System.err.println("Failed to write to log: " + ioException.getMessage());
            return false;
        }
    }

    // ----------- UTILITY METHODS -------------

    public void clearLog() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, false))) {
            writer.print("");
        } catch (IOException e) {
            System.err.println("Failed to clear log file: " + e.getMessage());
        }
    }

    public void testLogger() {
        debug("Test debug");
        info("Test info");
        warn("Test warn");
        error("Test error");

        try {
            throw new RuntimeException("Test exception");
        } catch (Exception ex) {
            error("Error with exception", ex);
        }
    }

    // ----------- ENUM -------------

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    // ----------- MAIN TEST -------------
    public static void main(String[] args) {
        LoggerService logger = LoggerService.getInstance();
        logger.testLogger();
    }
}
