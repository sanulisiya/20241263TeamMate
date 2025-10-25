package Service;

import model.Participant;
import java.util.*;

public class TeamBuilder {

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        Collections.shuffle(participants); // Randomize to add fairness

        int numTeams = (int) Math.ceil((double) participants.size() / teamSize);
        List<List<Participant>> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teams.add(new ArrayList<>());
        }

        // Keep track of skill totals for balancing
        int[] skillTotals = new int[numTeams];

        for (Participant p : participants) {
            int bestTeam = -1;
            double minSkillDiff = Double.MAX_VALUE;

            for (int t = 0; t < numTeams; t++) {
                List<Participant> team = teams.get(t);

                if (team.size() >= teamSize) continue; // team full
                if (countGame(team, p.getPreferredGame()) >= 2) continue; // game cap: max 2 per game

                int newSkillTotal = skillTotals[t] + p.getSkillLevel();
                double avgSkill = (double) newSkillTotal / (team.size() + 1);
                double diff = Math.abs(avgSkill - getOverallAvg(skillTotals, numTeams, teamSize));

                if (diff < minSkillDiff) {
                    minSkillDiff = diff;
                    bestTeam = t;
                }
            }

            // fallback if no team fits
            if (bestTeam == -1) {
                for (int t = 0; t < numTeams; t++) {
                    if (teams.get(t).size() < teamSize) {
                        bestTeam = t;
                        break;
                    }
                }
            }

            teams.get(bestTeam).add(p);
            skillTotals[bestTeam] += p.getSkillLevel();
        }

        return teams;
    }

    private static int countGame(List<Participant> team, String game) {
        int count = 0;
        for (Participant p : team) {
            if (p.getPreferredGame().equalsIgnoreCase(game)) count++;
        }
        return count;
    }

    private static double getOverallAvg(int[] skillTotals, int numTeams, int teamSize) {
        int total = 0;
        for (int s : skillTotals) total += s;
        return (double) total / (numTeams * teamSize);
    }
}
