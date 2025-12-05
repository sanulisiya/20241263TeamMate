package exception;

/**
 * Custom exception for file operation related errors
 */
public class FileOperationException extends RuntimeException {
    private final String filePath;
    private final String operation;

    //Constructor for creating the exception with a message, file path, and operation.
    public FileOperationException(String message, String filePath, String operation) {
        super(message);
        this.filePath = filePath;
        this.operation = operation;
    }
    //Constructor for creating the exception with a message, file path, operation, and a cause.
    public FileOperationException(String message, String filePath, String operation, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
        this.operation = operation;
    }

}