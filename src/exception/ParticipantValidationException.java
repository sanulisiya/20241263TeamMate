// File: exception/ParticipantValidationException.java
package exception;

/**
 * Custom exception for participant validation errors
 */
public class ParticipantValidationException extends RuntimeException {
    private final String fieldName;
    private final String invalidValue;

    //Constructor for creating the exception with a message, field name, and invalid value.
    public ParticipantValidationException(String message, String fieldName, String invalidValue) {
        super(message);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }
    //Constructor for creating the exception with a message, field name, invalid value, and cause.
    public ParticipantValidationException(String message, String fieldName, String invalidValue, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }
    //Getters
    public String getFieldName() {
        return fieldName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}