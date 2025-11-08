package Service;

import model.Participant;
import java.util.*;

public class TeamBuilder {

    private static final List<Participant> remainingParticipants = new ArrayList<>();

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        Collections.shuffle(participants, new Random());
        remainingParticipants.clear();

        if (participants.isEmpty() || teamSize <= 0) {
            return Collections.emptyList();
        }

        // Separate leaders and others
        List<Participant> leaders = new ArrayList<>();
        List<Participant> others = new ArrayList<>();

        for (Participant p : participants) {
            if (p.getPersonalityType().equalsIgnoreCase("Leader")) {
                leaders.add(p);
            } else {
                others.add(p);
            }
        }

        // Decide maximum possible teams based on number of leaders and total participants
        int possibleTeams = Math.min(leaders.size(), participants.size() / teamSize);
        if (possibleTeams == 0) {
            remainingParticipants.addAll(participants);
            return Collections.emptyList();
        }

        List<List<Participant>> teams = new ArrayList<>();
        for (int i = 0; i < possibleTeams; i++) {
            teams.add(new ArrayList<>());
        }

        // Assign one leader per team
        for (int i = 0; i < possibleTeams; i++) {
            teams.get(i).add(leaders.get(i));
        }

        // Compute global average skill
        double overallAvg = participants.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0);

        int[] skillTotals = new int[possibleTeams];
        for (int i = 0; i < possibleTeams; i++) {
            skillTotals[i] = teams.get(i).get(0).getSkillLevel();
        }

        // Fill teams intelligently with others
        for (Participant p : others) {
            int bestTeam = -1;
            double minDiff = Double.MAX_VALUE;

            for (int t = 0; t < possibleTeams; t++) {
                List<Participant> team = teams.get(t);
                if (team.size() >= teamSize) continue; // skip full team
                if (countGame(team, p.getPreferredGame()) >= 2) continue; // limit duplicate game

                double newAvg = (double) (skillTotals[t] + p.getSkillLevel()) / (team.size() + 1);
                double diff = Math.abs(newAvg - overallAvg);

                if (diff < minDiff) {
                    minDiff = diff;
                    bestTeam = t;
                }
            }

            if (bestTeam != -1) {
                teams.get(bestTeam).add(p);
                skillTotals[bestTeam] += p.getSkillLevel();
            } else {
                remainingParticipants.add(p);
            }
        }

        // Any partially filled teams that couldn’t reach teamSize → move to leftover pool
        Iterator<List<Participant>> it = teams.iterator();
        while (it.hasNext()) {
            List<Participant> team = it.next();
            if (team.size() < teamSize) {
                remainingParticipants.addAll(team);
                it.remove();
            }
        }

        // Return only complete teams
        return teams;
    }

    // Helper to count members with same preferred game
    private static int countGame(List<Participant> team, String game) {
        return (int) team.stream()
                .filter(p -> p.getPreferredGame().equalsIgnoreCase(game))
                .count();
    }

    // Getter for leftover participants
    public static List<Participant> getRemainingParticipants() {
        return new ArrayList<>(remainingParticipants);
    }
}
