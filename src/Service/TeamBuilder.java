package Service;

import model.Participant;
import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    // --- CONSTRAINTS ---
    private static final List<Participant> remainingParticipants = new ArrayList<>();
    private static final int GAME_CAP = 2; // Max participants with the same game per team
    private static final int MAX_THINKERS = 2; // Max Thinkers allowed
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
            // Note: roleCounts is still populated, but ignored in the leftover assignment logic
            roleCounts.merge(p.getPersonalityType().toLowerCase(), 1, Integer::sum);
        }
    }

    // =========================================================================
    // PRIMARY METHOD: FORMS TEAMS FOLLOWING ALL CONSTRAINTS (Leader, Thinker, Skill Balance)
    // =========================================================================
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
        List<Participant> stage2Thinkers = new ArrayList<>(thinkers);
        thinkers.clear();

        for (Participant thinker : stage2Thinkers) {
            TeamState bestTeam = findBestTeamForParticipant(teamStates, thinker, overallAvg, teamSize);

            if (bestTeam != null) {
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
    // NEW METHOD: FORMS TEAMS FROM LEFTOVERS (Skill Balance and Game Cap Only)
    // =========================================================================
    public static List<List<Participant>> formLeftoverTeams(int teamSize) {
        List<Participant> pool = getRemainingParticipants();

        // If not enough participants for at least one full team
        if (pool.size() < teamSize) {
            // remainingParticipants is already updated by getRemainingParticipants()
            return Collections.emptyList();
        }

        // 1. Calculate the average skill of the current pool
        double poolAvgSkill = pool.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0);

        // 2. Determine possible teams and initialize TeamState list
        int maxNewTeams = pool.size() / teamSize;
        List<TeamState> newTeams = new ArrayList<>();

        // Sort pool by skill level (descending) to balance high-skill players first
        pool.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        // 3. Initialize teams with one member (high skill)
        // Use a temporary list to hold members that are used to start a team
        List<Participant> tempPool = new ArrayList<>(pool);
        pool.clear();

        for (int i = 0; i < maxNewTeams; i++) {
            if (i < tempPool.size()) {
                TeamState state = new TeamState(newTeams.size() + 100); // Unique ID offset
                state.addMember(tempPool.remove(0)); // Remove highest skilled member
                newTeams.add(state);
            }
        }

        // The rest of the tempPool goes back to the main pool for assignment
        pool.addAll(tempPool);


        // 4. Fill remaining slots using skill balance and GAME_CAP
        List<Participant> unassigned = new ArrayList<>();
        Collections.shuffle(pool, new Random()); // Shuffle the remaining pool

        for (Participant p : pool) {
            TeamState bestTeam = findBestLeftoverTeam(newTeams, p, poolAvgSkill, teamSize);

            if (bestTeam != null) {
                bestTeam.addMember(p);
            } else {
                unassigned.add(p);
            }
        }

        // 5. Finalize teams and update remainingParticipants
        List<List<Participant>> finalNewTeams = new ArrayList<>();
        remainingParticipants.clear(); // Clear the old static pool list
        remainingParticipants.addAll(unassigned); // Add unassigned members back

        for (TeamState team : newTeams) {
            if (team.members.size() == teamSize) {
                finalNewTeams.add(team.members);
            } else {
                // If the new team is incomplete, its members are also considered leftovers
                remainingParticipants.addAll(team.members);
            }
        }

        return finalNewTeams;
    }


    // =========================================================================
    // HELPER METHOD: ENFORCES ALL CONSTRAINTS (For initial Team Building)
    // =========================================================================
    private static TeamState findBestTeamForParticipant(List<TeamState> teamStates, Participant p, double overallAvg, int teamSize) {

        String pRole = p.getPersonalityType().toLowerCase();

        List<TeamState> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        for (TeamState team : teamStates) {

            // --- HARD CONSTRAINTS ---
            if (team.members.size() >= teamSize) continue;
            if (countGame(team.members, p.getPreferredGame()) >= GAME_CAP) continue;

            // FIX: STRICTLY PREVENT 3RD THINKER ASSIGNMENT
            if (pRole.equals("thinker") && team.roleCounts.getOrDefault("thinker", 0) >= MAX_THINKERS) {
                continue;
            }

            // --- SOFT CONSTRAINTS / DIVERSITY CHECKS ---
            Map<String, Integer> roles = team.roleCounts;
            int uniqueRoles = roles.size();
            boolean addsNewRole = !roles.containsKey(pRole);

            // Avoid adding a player that *only* increases a redundant role count
            if (!addsNewRole && uniqueRoles >= MIN_UNIQUE_ROLES) {
                if (pRole.equals("leader") || (pRole.equals("thinker"))) {
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

    // =========================================================================
    // HELPER METHOD: Simplified Team Finder (Skill Balance and Game Cap Only)
    // =========================================================================
    private static TeamState findBestLeftoverTeam(List<TeamState> teamStates, Participant p, double overallAvg, int teamSize) {
        List<TeamState> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        for (TeamState team : teamStates) {

            // --- HARD CONSTRAINTS (Only Team Size and Game Cap remain) ---
            if (team.members.size() >= teamSize) continue;
            if (countGame(team.members, p.getPreferredGame()) >= GAME_CAP) continue;

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