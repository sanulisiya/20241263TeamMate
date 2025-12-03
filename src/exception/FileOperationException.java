// File: exception/FileOperationException.java
package exception;

/**
 * Custom exception for file operation related errors
 */
public class FileOperationException extends RuntimeException {
    private final String filePath;
    private final String operation;

    public FileOperationException(String message, String filePath, String operation) {
        super(message);
        this.filePath = filePath;
        this.operation = operation;
    }

    public FileOperationException(String message, String filePath, String operation, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
        this.operation = operation;
    }

}