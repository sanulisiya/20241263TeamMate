package model;

public enum GameRole {
    // Each constant has an assigned description
    STRATEGIST(
            "Focuses on tactics and planning. Keeps the bigger picture in mind during gameplay."
    ),
    ATTACKER(
            "Frontline player. Good reflexes, offensive tactics, quick execution."
    ),
    DEFENDER(
            "Protects and supports team stability. Good under pressure and team-focused."
    ),
    SUPPORTER(
            "Jack-of-all-trades. Adapts roles, ensures smooth coordination."
    ),
    COORDINATOR(
            "Communication lead. Keeps the team informed and organized in real time."
    );

    // Private field to hold the description for each role
    private final String description;

    // Enum constructor
    private GameRole(String description) {
        this.description = description;
    }

    // Public getter method
    public String getDescription() {
        return description;
    }

    public boolean equalsIgnoreCase(GameRole role) {

        return false;
    }
}