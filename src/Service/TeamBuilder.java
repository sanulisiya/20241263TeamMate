// Corrected TeamBuilder.java

package Service;

import model.Participant;
import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    private static final List<Participant> remainingParticipants = new ArrayList<>();
    private static final int GAME_CAP = 2; // Max participants with the same game per team
    private static final int MAX_THINKERS = 2; // ***NEW HARD CONSTRAINT*** Max Thinkers allowed
    private static final int MIN_UNIQUE_ROLES = 3;

    // Helper class to hold team state and simplify the team list
    private static class TeamState {
        List<Participant> members = new ArrayList<>();
        Map<String, Integer> roleCounts = new HashMap<>();
        int skillTotal = 0;
        int teamId;

        public TeamState(int id) {
            this.teamId = id;
        }

        public void addMember(Participant p) {
            members.add(p);
            skillTotal += p.getSkillLevel();
            roleCounts.merge(p.getPersonalityType().toLowerCase(), 1, Integer::sum);
        }
    }

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        remainingParticipants.clear();

        if (participants.isEmpty() || teamSize <= 0) {
            return Collections.emptyList();
        }

        // 1. Separate participants and prepare 'others' list
        Map<String, List<Participant>> rolesMap = participants.stream()
                .collect(Collectors.groupingBy(p -> p.getPersonalityType().toLowerCase()));

        List<Participant> leaders = rolesMap.getOrDefault("leader", new ArrayList<>());
        List<Participant> thinkers = rolesMap.getOrDefault("thinker", new ArrayList<>());
        List<Participant> balanced = rolesMap.getOrDefault("balanced", new ArrayList<>());
        List<Participant> motivators = rolesMap.getOrDefault("motivator", new ArrayList<>());

        List<Participant> remainingOthers = new ArrayList<>();
        remainingOthers.addAll(balanced);
        remainingOthers.addAll(motivators);
        Collections.shuffle(remainingOthers, new Random());

        // 2. Determine teams and overall average
        int possibleTeams = Math.min(leaders.size(), participants.size() / teamSize);
        if (possibleTeams == 0) {
            remainingParticipants.addAll(participants);
            return Collections.emptyList();
        }

        double overallAvg = participants.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0);

        List<TeamState> teamStates = new ArrayList<>();

        // 3. Initialize teams with one Leader each
        Collections.shuffle(leaders, new Random());
        for (int i = 0; i < possibleTeams; i++) {
            TeamState state = new TeamState(i);
            state.addMember(leaders.get(i));
            teamStates.add(state);
        }
        leaders = leaders.subList(possibleTeams, leaders.size());
        remainingParticipants.addAll(leaders);

        // 4. STAGE 1: ASSIGN MANDATORY 1 THINKER PER TEAM
        Collections.shuffle(thinkers, new Random());
        Iterator<Participant> thinkerIterator = thinkers.iterator();

        for (TeamState team : teamStates) {
            if (!thinkerIterator.hasNext()) break;

            Participant thinker = thinkerIterator.next();

            if (team.members.size() < teamSize && countGame(team.members, thinker.getPreferredGame()) < GAME_CAP) {
                team.addMember(thinker);
                thinkerIterator.remove();
            } else {
                remainingOthers.add(0, thinker);
            }
        }

        // 5. STAGE 2: ASSIGN OPTIONAL 2ND THINKER
        // Use a list copy to safely iterate while adding others back to remainingOthers
        List<Participant> stage2Thinkers = new ArrayList<>(thinkers);
        thinkers.clear(); // Clear the main thinkers list after copying for final leftover collection

        for (Participant thinker : stage2Thinkers) {
            // Find the best team for this 2nd thinker attempt
            TeamState bestTeam = findBestTeamForParticipant(teamStates, thinker, overallAvg, teamSize);

            if (bestTeam != null) {
                // *** THE CORE FIX IS IN findBestTeamForParticipant, BUT WE DOUBLE-CHECK HERE ***
                if (bestTeam.roleCounts.getOrDefault("thinker", 0) < MAX_THINKERS) {
                    bestTeam.addMember(thinker);
                } else {
                    remainingOthers.add(0, thinker);
                }
            } else {
                remainingOthers.add(0, thinker);
            }
        }

        // 6. STAGE 3: ASSIGN REMAINING BALANCED/MOTIVATORS/LEFTOVER THINKERS
        for (Participant p : remainingOthers) {
            TeamState bestTeam = findBestTeamForParticipant(teamStates, p, overallAvg, teamSize);

            if (bestTeam != null) {
                bestTeam.addMember(p);
            } else {
                remainingParticipants.add(p);
            }
        }

        // 7. Finalize teams and collect true leftovers
        List<List<Participant>> finalTeams = new ArrayList<>();
        for (TeamState team : teamStates) {
            if (team.members.size() == teamSize) {
                finalTeams.add(team.members);
            } else {
                remainingParticipants.addAll(team.members);
            }
        }

        return finalTeams;
    }

    // =========================================================================
    // MODIFIED HELPER METHOD: ENFORCES MAX THINKERS AS A HARD CONSTRAINT
    // =========================================================================
    private static TeamState findBestTeamForParticipant(List<TeamState> teamStates, Participant p, double overallAvg, int teamSize) {

        // Get role once for comparison
        String pRole = p.getPersonalityType().toLowerCase();

        List<TeamState> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        for (TeamState team : teamStates) {

            // --- HARD CONSTRAINTS ---
            if (team.members.size() >= teamSize) continue;
            if (countGame(team.members, p.getPreferredGame()) >= GAME_CAP) continue;

            // *** FIX: STRICTLY PREVENT 3RD THINKER ASSIGNMENT ***
            if (pRole.equals("thinker") && team.roleCounts.getOrDefault("thinker", 0) >= MAX_THINKERS) {
                continue;
            }
            // ------------------------

            // --- SOFT CONSTRAINTS / DIVERSITY CHECKS ---
            Map<String, Integer> roles = team.roleCounts;
            int uniqueRoles = roles.size();
            boolean addsNewRole = !roles.containsKey(pRole);

            // Avoid adding a player that *only* increases a redundant role count
            if (!addsNewRole && uniqueRoles >= MIN_UNIQUE_ROLES) {
                // Still allow adding Balanced/Motivators to fill team slots
                if (pRole.equals("leader") || (pRole.equals("thinker"))) {
                    // This check is mostly redundant now due to the hard constraint above,
                    // but kept here for logical flow regarding leaders/thinkers.
                    continue;
                }
            }

            // Calculate skill balance score
            double newAvg = (double) (team.skillTotal + p.getSkillLevel()) / (team.members.size() + 1);
            double diff = Math.abs(newAvg - overallAvg);

            if (diff < minDiff) {
                validTeams.clear();
                validTeams.add(team);
                minDiff = diff;
            } else if (diff == minDiff) {
                validTeams.add(team);
            }
        }

        if (!validTeams.isEmpty()) {
            return validTeams.get(new Random().nextInt(validTeams.size()));
        }
        return null;
    }

    // Count participants in a team with the same game
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