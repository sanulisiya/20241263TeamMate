package model;

import java.util.*;
import java.util.stream.Collectors;

public class Team {
    private final int teamId;
    private final List<Participant> members = new ArrayList<>();
    private int totalSkill = 0;

    // --- NEW CACHING FIELDS ---
    // Role counts (e.g., "leader": 1, "thinker": 2)
    private final Map<String, Integer> roleCounts = new HashMap<>();
    // Game counts (e.g., "valorant": 3, "league of legends": 1)
    private final Map<String, Integer> gameCounts = new HashMap<>();
    // --------------------------

    public Team(int teamId) {
        this.teamId = teamId;
    }

    // Add member and update skill and cached counts
    public void addMember(Participant p) {
        if (p == null) return;

        members.add(p);
        totalSkill += p.getSkillLevel();

        // Update cached role count
        String role = safeRole(p);
        roleCounts.merge(role, 1, Integer::sum);

        // Update cached game count
        String game = safeGame(p);
        gameCounts.merge(game, 1, Integer::sum);
    }

    // Helper to safely get the lower-cased role string
    private String safeRole(Participant p) {
        if (p == null || p.getPersonalityType() == null) return "unknown";
        return p.getPersonalityType().toString().toLowerCase();
    }

    // Helper to safely get the lower-cased game string
    private String safeGame(Participant p) {
        if (p == null || p.getPreferredGame() == null) return "unknown";
        return p.getPreferredGame().toLowerCase();
    }

    public int getTeamId() {
        return teamId;
    }

    public List<Participant> getMembers() {
        return members;
    }

    public int getTotalSkill() {
        return totalSkill;
    }

    public double getAverageSkill() {
        return members.isEmpty() ? 0 : (double) totalSkill / members.size();
    }

    // --- NEW METHODS FOR TEAMBUILDER EFFICIENCY ---

    /**
     * Retrieves the cached count of participants with a specific role.
     * @param role The role type (e.g., "leader", "thinker")
     * @return The count of members with that role.
     */
    public int getRoleCount(String role) {
        return roleCounts.getOrDefault(role.toLowerCase(), 0);
    }

    /**
     * Retrieves the cached count of participants who prefer a specific game.
     * @param game The preferred game string.
     * @return The count of members who prefer that game.
     */
    public int getGameCount(String game) {
        return gameCounts.getOrDefault(game.toLowerCase(), 0);
    }

    /**
     * Returns the number of unique roles currently on the team.
     * @return The size of the roleCounts map.
     */
    public int getUniqueRoleCount() {
        return roleCounts.size();
    }

    @Override
    public String toString() {
        return String.format(
                "Team %d | Size: %d | Avg Skill: %.2f | Members: %s",
                teamId,
                members.size(),
                getAverageSkill(),
                members.stream()
                        .map(Participant::getName)
                        .collect(Collectors.joining(", "))
        );
    }
}