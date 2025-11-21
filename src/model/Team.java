package model;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private int teamId;
    private List<Participant> members;
    private int totalSkill;
    private double avgSkill;

    // Constructor
    public Team(int teamId) {
        this.teamId = teamId;
        this.members = new ArrayList<>();
        this.totalSkill = 0;
        this.avgSkill = 0.0;
    }

    // Add a participant
    public void addMember(Participant participant) {
        members.add(participant);
        totalSkill += participant.getSkillLevel();
        avgSkill = (double) totalSkill / members.size();
    }

    // Getters
    public int getTeamId() {
        return teamId;
    }

    public List<Participant> getMembers() {
        return members;
    }

    public double getAvgSkill() {
        return avgSkill;
    }

    public int getTotalSkill() {
        return totalSkill;
    }

    // Count how many members have the same game
    public long countGame(String game) {
        return members.stream()
                .filter(p -> p.getPreferredGame().equalsIgnoreCase(game))
                .count();
    }

    // Count how many members have the same role
    public long countRole(RoleType role) {
        return members.stream()
                .filter(p -> p.getPreferredRole().equals(role))
                .count();
    }

    // Calculate average compatibility score with a new member
    public double calculateCompatibility(Participant newMember) {
        if (members.isEmpty()) return 100.0;

        double total = 0;
        for (Participant p : members) {
            total += p.calculateCompatibility(newMember);
        }
        return total / members.size();
    }

    // Diversity bonus (adds value if new member adds variety)
    public double calculateDiversityBonus(Participant newMember) {
        boolean hasSameGame = members.stream()
                .anyMatch(p -> p.getPreferredGame().equalsIgnoreCase(newMember.getPreferredGame()));
        boolean hasSameRole = members.stream()
                .anyMatch(p -> p.getPreferredRole().equalsIgnoreCase(newMember.getPreferredRole()));
        boolean hasSamePersonality = members.stream()
                .anyMatch(p -> p.getPersonalityType().equals(newMember.getPersonalityType()));

        double bonus = 0;
        if (!hasSameGame) bonus += 30;
        if (!hasSameRole) bonus += 30;
        if (!hasSamePersonality) bonus += 40;

        return bonus; // max 100
    }

    // Helper: team size
    public int size() {
        return members.size();
    }

    // Pretty print team info
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Team " + teamId + " (Avg Skill: " + String.format("%.2f", avgSkill) + ")\n");
        for (Participant p : members) {
            sb.append("  - ").append(p.toString()).append("\n");
        }
        return sb.toString();
    }
}