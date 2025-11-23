package model;

public class Participant {
    private String id;
    private String name;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private RoleType preferredRole;  // Changed from String to GameRole
    private int personalityScore;
    private PersonalityType personalityType;  // Changed from String to PersonalityType
    private String teamNumber = "";
    private String availability; // optional

    // Constructor
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       RoleType preferredRole, int personalityScore, PersonalityType personalityType) {
        // Validate critical fields
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Participant ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Participant name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Participant email cannot be null or empty");
        }
        if (skillLevel < 1 || skillLevel > 10) {
            throw new IllegalArgumentException("Skill level must be between 1 and 10");
        }
        if (personalityScore < 0 || personalityScore > 100) {
            throw new IllegalArgumentException("Personality score must be between 0 and 100");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.email = email.trim();
        this.preferredGame = (preferredGame != null) ? preferredGame.trim() : "";
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }

    // Overloaded constructor with availability
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       RoleType preferredRole, int personalityScore, PersonalityType personalityType, String availability) {
        this(id, name, email, preferredGame, skillLevel, preferredRole, personalityScore, personalityType);
        this.availability = (availability != null) ? availability.trim() : null;
    }

    // ---------------- Getters ----------------
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPreferredGame() {
        return preferredGame;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public RoleType getPreferredRole() {
        return preferredRole;
    }

    public int getPersonalityScore() {
        return personalityScore;
    }

    public PersonalityType getPersonalityType() {
        return personalityType;
    }

    public String getAvailability() {
        return availability;
    }

    public String getTeamNumber() {
        return teamNumber;
    }

    // ---------------- Setters ----------------
    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        this.id = id.trim();
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name.trim();
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        this.email = email.trim();
    }

    public void setPreferredGame(String preferredGame) {
        this.preferredGame = (preferredGame != null) ? preferredGame.trim() : "";
    }

    public void setSkillLevel(int skillLevel) {
        if (skillLevel < 1 || skillLevel > 10) {
            throw new IllegalArgumentException("Skill level must be between 1 and 10");
        }
        this.skillLevel = skillLevel;
    }

    public void setPreferredRole(RoleType preferredRole) {
        this.preferredRole = preferredRole;
    }

    public void setPersonalityScore(int personalityScore) {
        if (personalityScore < 0 || personalityScore > 100) {
            throw new IllegalArgumentException("Personality score must be between 0 and 100");
        }
        this.personalityScore = personalityScore;
    }

    public void setPersonalityType(PersonalityType personalityType) {
        this.personalityType = personalityType;
    }

    public void setAvailability(String availability) {
        this.availability = (availability != null) ? availability.trim() : null;
    }

    public void setTeamNumber(String teamNumber) {
        this.teamNumber = (teamNumber != null) ? teamNumber.trim() : "";
    }

    // ---------------- Utility Methods ----------------
    public double calculateCompatibility(Participant other) {
        if (other == null) {
            return 0.0;
        }
        return 100 - Math.abs(this.personalityScore - other.personalityScore);
    }

    public boolean playsSameGame(Participant other) {
        if (other == null || this.preferredGame == null || other.preferredGame == null) {
            return false;
        }
        return this.preferredGame.equalsIgnoreCase(other.preferredGame);
    }

    // Helper method to get role as string (for backward compatibility)
    public String getPreferredRoleAsString() {
        return (preferredRole != null) ? preferredRole.name() : "";
    }

    // Helper method to get personality type as string (for backward compatibility)
    public String getPersonalityTypeAsString() {
        return (personalityType != null) ? personalityType.name() : "";
    }

    // Validation method
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() &&
                name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                skillLevel >= 1 && skillLevel <= 10 &&
                personalityScore >= 0 && personalityScore <= 100 &&
                preferredRole != null &&
                personalityType != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" | ").append(name)
                .append(" | Game: ").append(preferredGame)
                .append(" | Skill: ").append(skillLevel)
                .append(" | Role: ").append(preferredRole != null ? preferredRole.name() : "UNKNOWN")
                .append(" | Score: ").append(personalityScore)
                .append(" | Type: ").append(personalityType != null ? personalityType.name() : "UNKNOWN");

        if (availability != null && !availability.isEmpty()) {
            sb.append(" | Availability: ").append(availability);
        }
//        if (teamNumber != null && !teamNumber.isEmpty()) {
//            sb.append(" | Team: ").append(teamNumber);
//        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Participant that = (Participant) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}