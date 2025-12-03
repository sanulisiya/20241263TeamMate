// File: exception/TeamFormationException.java
package exception;

/**
 * Custom exception for team formation related errors
 */
public class TeamFormationException extends RuntimeException {
    private final String errorCode;

    public TeamFormationException(String message, String formationError, Exception e) {
        super(message);
        this.errorCode = "TEAM_FORMATION_ERROR";
    }

    public TeamFormationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TeamFormationException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}