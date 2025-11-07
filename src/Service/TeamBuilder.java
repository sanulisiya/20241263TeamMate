package Service;

import model.Participant;
import java.util.*;

public class TeamBuilder {

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        // Shuffle to add randomness and fairness
        Collections.shuffle(participants, new Random());

        int numTeams = (int) Math.ceil((double) participants.size() / teamSize);
        List<List<Participant>> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teams.add(new ArrayList<>());
        }

        int[] skillTotals = new int[numTeams];
        double overallAvgSkill = participants.stream().mapToInt(Participant::getSkillLevel).average().orElse(0);

        for (Participant p : participants) {
            int bestTeam = -1;
            double bestScore = Double.MIN_VALUE; // Higher = better match

            for (int t = 0; t < numTeams; t++) {
                List<Participant> team = teams.get(t);

                if (team.size() >= teamSize) continue; // team full
                if (countGame(team, p.getPreferredGame()) >= 2) continue; // limit duplicate games
                if (countRole(team, p.getPreferredRole()) >= 2) continue; // limit duplicate roles

                // Compute factors
                double newAvgSkill = (skillTotals[t] + p.getSkillLevel()) / (double) (team.size() + 1);
                double skillBalanceScore = 100 - Math.abs(newAvgSkill - overallAvgSkill); // closer to avg = better
                double personalityCompatibility = calcTeamCompatibility(team, p); // avg personality match
                double diversityBonus = calcDiversityBonus(team, p); // + if new game/role/personality type

                // Weighted score (you can tune weights)
                double totalScore = (0.5 * skillBalanceScore) + (0.3 * personalityCompatibility) + (0.2 * diversityBonus);

                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestTeam = t;
                }
            }

            // Fallback: if no team fits all criteria
            if (bestTeam == -1) {
                for (int t = 0; t < numTeams; t++) {
                    if (teams.get(t).size() < teamSize) {
                        bestTeam = t;
                        break;
                    }
                }
            }

            // Assign participant
            teams.get(bestTeam).add(p);
            skillTotals[bestTeam] += p.getSkillLevel();
        }

        return teams;
    }

    // ---------------- Helper Methods ----------------

    private static int countGame(List<Participant> team, String game) {
        return (int) team.stream()
                .filter(p -> p.getPreferredGame().equalsIgnoreCase(game))
                .count();
    }

    private static int countRole(List<Participant> team, String role) {
        return (int) team.stream()
                .filter(p -> p.getPreferredRole().equalsIgnoreCase(role))
                .count();
    }

    // Calculate how compatible this participant is with current team (based on personality score)
    private static double calcTeamCompatibility(List<Participant> team, Participant newMember) {
        if (team.isEmpty()) return 100.0; // Perfect compatibility with empty team

        double totalCompatibility = 0;
        for (Participant p : team) {
            totalCompatibility += p.calculateCompatibility(newMember);
        }
        return totalCompatibility / team.size(); // average
    }

    // Reward if participant adds diversity in game, role, or personality type
    private static double calcDiversityBonus(List<Participant> team, Participant newMember) {
        boolean hasSameGame = team.stream().anyMatch(p -> p.getPreferredGame().equalsIgnoreCase(newMember.getPreferredGame()));
        boolean hasSameRole = team.stream().anyMatch(p -> p.getPreferredRole().equalsIgnoreCase(newMember.getPreferredRole()));
        boolean hasSamePersonality = team.stream().anyMatch(p -> p.getPersonalityType().equalsIgnoreCase(newMember.getPersonalityType()));

        double bonus = 0;
        if (!hasSameGame) bonus += 30;
        if (!hasSameRole) bonus += 30;
        if (!hasSamePersonality) bonus += 40;

        return bonus; // max 100
    }
}
