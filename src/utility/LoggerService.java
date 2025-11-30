package utility;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

public class LoggerService {

    private static LoggerService instance;
    private static final Object lock = new Object();

    private final String logFile;
    private final DateTimeFormatter timestampFormat;
    private final ReentrantLock writeLock;

    // ----------- SIMPLE CONSTRUCTOR -----------
    private LoggerService() {
        this.logFile = getLogFilePath();
        this.timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.writeLock = new ReentrantLock();
        ensureLogDirectoryExists();
    }

    // ----------- GET INSTANCE -----------
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

    // ----------- BASIC LOG METHODS -----------
    public void debug(String msg) {
        log("DEBUG", msg, null);
    }

    public void info(String msg) {
        log("INFO", msg, null);
    }

    public void warn(String msg) {
        log("WARN", msg, null);
    }

    public void error(String msg) {
        log("ERROR", msg, null);
    }

    public void error(String msg, Exception e) {
        log("ERROR", msg, e);
    }

    // ----------- CORE LOG FUNCTION -----------
    private void log(String level, String message, Exception e) {
        String timestamp = LocalDateTime.now().format(timestampFormat);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);

        writeToFile(logEntry, e);

        // Also print errors to console
        if ("ERROR".equals(level)) {
            System.err.println("❌ " + message);
        }
    }

    // ----------- FILE WRITING -----------
    private void writeToFile(String logEntry, Exception e) {
        writeLock.lock();
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logEntry);

            if (e != null) {
                writer.println("Exception: " + e.getMessage());
                // Simple stack trace
                e.printStackTrace(writer);
            }
        } catch (IOException ioException) {
            System.err.println("Logger failed: " + ioException.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    // ----------- UTILITY METHODS -----------
    private static String getLogFilePath() {
        String projectDir = System.getProperty("user.dir");
        return projectDir + File.separator + "teammate_system.log";
    }

    private void ensureLogDirectoryExists() {
        try {
            File logFileObj = new File(this.logFile);
            File parentDir = logFileObj.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    // ----------- SIMPLE TEST -----------
    public void testLogger() {
        debug("Test debug");
        info("Test info");
        warn("Test warn");
        error("Test error");

        try {
            throw new RuntimeException("Test exception");
        } catch (Exception ex) {
            error("Test with exception", ex);
        }
    }
}