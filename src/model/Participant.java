package model;

public class Participant {
    private String id;
    private String name;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private String preferredRole;
    private int personalityScore;
    private String personalityType;
    private String teamNumber = ""; //  changed from int to String

    private String availability; // optional

    // Constructor
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       String preferredRole, int personalityScore, String personalityType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }

    // Overloaded constructor with availability
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       String preferredRole, int personalityScore, String personalityType, String availability) {
        this(id, name, email, preferredGame, skillLevel, preferredRole, personalityScore, personalityType);
        this.availability = availability;
    }

    // ---------------- Getters ----------------
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPreferredGame() { return preferredGame; }
    public int getSkillLevel() { return skillLevel; }
    public String getPreferredRole() { return preferredRole; }
    public int getPersonalityScore() { return personalityScore; }
    public String getPersonalityType() { return personalityType; }
    public String getAvailability() { return availability; }
    public String getTeamNumber() { return teamNumber; } //  new getter

    // ---------------- Setters ----------------
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPreferredGame(String preferredGame) { this.preferredGame = preferredGame; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public void setPreferredRole(String preferredRole) { this.preferredRole = preferredRole; }
    public void setPersonalityScore(int personalityScore) { this.personalityScore = personalityScore; }
    public void setPersonalityType(String personalityType) { this.personalityType = personalityType; }
    public void setAvailability(String availability) { this.availability = availability; }
    public void setTeamNumber(String teamNumber) { this.teamNumber = teamNumber; } //  new setter

    // ---------------- Utility Methods ----------------
    public double calculateCompatibility(Participant other) {
        return 100 - Math.abs(this.personalityScore - other.personalityScore);
    }

    public boolean playsSameGame(Participant other) {
        return this.preferredGame.equalsIgnoreCase(other.preferredGame);
    }

    @Override
    public String toString() {
        return id + " | " + name +
                " | Game: " + preferredGame +
                " | Skill: " + skillLevel +
                " | Role: " + preferredRole +
                " | Score: " + personalityScore +
                " | Type: " + personalityType +
                (availability != null ? " | Availability: " + availability : "") +
                (!teamNumber.isEmpty() ? " | Team: " + teamNumber : "");
    }
}