package Service;

import model.Participant;
import java.util.*;

public class TeamBuilder {

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        Collections.shuffle(participants); // Randomize for fairness

        int numTeams = (int) Math.ceil((double) participants.size() / teamSize);
        List<List<Participant>> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teams.add(new ArrayList<>());
        }

        int[] skillTotals = new int[numTeams];

        // Precompute overall average skill
        double overallAvg = participants.stream().mapToInt(Participant::getSkillLevel).average().orElse(0);

        for (Participant p : participants) {
            int bestTeam = -1;
            double minSkillDiff = Double.MAX_VALUE;

            for (int t = 0; t < numTeams; t++) {
                List<Participant> team = teams.get(t);

                if (team.size() >= teamSize) continue;
                if (countGame(team, p.getPreferredGame()) >= 2) continue;

                double newAvg = (double) (skillTotals[t] + p.getSkillLevel()) / (team.size() + 1);
                double diff = Math.abs(newAvg - overallAvg);

                if (diff < minSkillDiff) {
                    minSkillDiff = diff;
                    bestTeam = t;
                }
            }

            // Fallback if no team fits perfectly
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
        return (int) team.stream().filter(p -> p.getPreferredGame().equalsIgnoreCase(game)).count();
    }
}
