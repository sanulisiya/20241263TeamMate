package Service;

import model.Participant;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

public class TeamBuilder {

    // NOTE: This list needs synchronization if accessed by multiple threads outside of the parallel stream
    private static final List<Participant> remainingParticipants = Collections.synchronizedList(new ArrayList<>());
    private static final int GAME_CAP = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MIN_UNIQUE_ROLES = 3;

    private static class TeamState {
        // Use synchronized collections if members were accessed outside synchronized blocks
        List<Participant> members = new ArrayList<>();
        Map<String, Integer> roleCounts = new HashMap<>();
        int skillTotal = 0;
        int teamId;

        public TeamState(int id) {
            this.teamId = id;
        }

        // Method is *not* synchronized because it's called inside a synchronized block in formTeams
        public void addMember(Participant p) {
            try {
                if (p != null) {
                    members.add(p);
                    skillTotal += safeSkill(p);
                    String role = safeRole(p);
                    roleCounts.merge(role, 1, Integer::sum);
                }
            } catch (Exception ignored) {}
        }
    }

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        remainingParticipants.clear();
        if (participants == null || participants.isEmpty() || teamSize <= 0) return Collections.emptyList();

        Map<String, List<Participant>> rolesMap = participants.stream()
                .collect(Collectors.groupingBy(p -> safeRole(p)));

        // Initial setup of team leaders and pools (sequential)
        List<Participant> leaders = new ArrayList<>(rolesMap.getOrDefault("leader", new ArrayList<>()));
        List<Participant> thinkers = new ArrayList<>(rolesMap.getOrDefault("thinker", new ArrayList<>()));
        List<Participant> balanced = new ArrayList<>(rolesMap.getOrDefault("balanced", new ArrayList<>()));
        List<Participant> motivators = new ArrayList<>(rolesMap.getOrDefault("motivator", new ArrayList<>()));

        List<Participant> remainingOthers = new ArrayList<>();
        remainingOthers.addAll(balanced);
        remainingOthers.addAll(motivators);
        Collections.shuffle(remainingOthers, new Random());

        int possibleTeams = Math.min(leaders.size(), participants.size() / teamSize);
        if (possibleTeams == 0) {
            remainingParticipants.addAll(participants);
            return Collections.emptyList();
        }

        double overallAvg = participants.stream()
                .mapToInt(TeamBuilder::safeSkill)
                .average()
                .orElse(0);

        List<TeamState> teamStates = new ArrayList<>();
        Collections.shuffle(leaders, new Random());
        for (int i = 0; i < possibleTeams; i++) {
            TeamState state = new TeamState(i);
            state.addMember(leaders.get(i));
            teamStates.add(state);
        }
        if (leaders.size() > possibleTeams) remainingParticipants.addAll(leaders.subList(possibleTeams, leaders.size()));

        // Assign one thinker per team (sequential)
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
                thinkerIterator.remove();
            }
        }
        remainingOthers.addAll(thinkers); // Add any unassigned thinkers back to the pool

        // --- Multi-threaded Assignment using Parallel Stream ---

        // This stream processes the assignment of 'remainingOthers' concurrently
        // The synchronized block prevents race conditions when modifying shared TeamState objects.
        remainingOthers.parallelStream().forEach(p -> {
            TeamState bestTeam = findBestTeamForParticipant(teamStates, p, overallAvg, teamSize);

            if (bestTeam != null) {
                // Synchronize access to the specific team object being modified
                synchronized (bestTeam) {
                    // Double-check condition inside the lock just in case another thread filled it
                    if (bestTeam.members.size() < teamSize) {
                        bestTeam.addMember(p);
                    } else {
                        // If blocked and team is full, add to remaining list (needs synchronization)
                        remainingParticipants.add(p);
                    }
                }
            } else {
                // Synchronize access to the static remainingParticipants list
                remainingParticipants.add(p);
            }
        });

        // --- End Multi-threaded Assignment ---

        List<List<Participant>> finalTeams = new ArrayList<>();
        // Collect results sequentially
        for (TeamState team : teamStates) {
            if (team.members.size() == teamSize) finalTeams.add(new ArrayList<>(team.members));
            else remainingParticipants.addAll(team.members);
        }
        return finalTeams;
    }

    // formLeftoverTeams remains sequential for simplicity, but could also be updated

    public static List<List<Participant>> formLeftoverTeams(int teamSize) {
        List<Participant> pool = new ArrayList<>(getRemainingParticipants());
        if (teamSize <= 0 || pool.size() < teamSize) return Collections.emptyList();

        double poolAvgSkill = pool.stream()
                .mapToInt(TeamBuilder::safeSkill)
                .average()
                .orElse(0);

        int maxNewTeams = pool.size() / teamSize;
        List<TeamState> newTeams = new ArrayList<>();
        pool.sort(Comparator.comparingInt(TeamBuilder::safeSkill).reversed());
        List<Participant> tempPool = new ArrayList<>(pool);
        pool.clear();

        for (int i = 0; i < maxNewTeams; i++) {
            if (tempPool.isEmpty()) break;
            TeamState state = new TeamState(newTeams.size() + 100);
            state.addMember(tempPool.remove(0));
            newTeams.add(state);
        }
        pool.addAll(tempPool);
        Collections.shuffle(pool, new Random());

        List<Participant> unassigned = new ArrayList<>();

        // Optional: Could use parallelStream here, but requires synchronization on newTeams elements
        for (Participant p : pool) {
            TeamState bestTeam = findBestLeftoverTeam(newTeams, p, poolAvgSkill, teamSize);
            if (bestTeam != null) bestTeam.addMember(p);
            else unassigned.add(p);
        }

        List<List<Participant>> finalNewTeams = new ArrayList<>();
        remainingParticipants.clear();
        for (TeamState team : newTeams) {
            if (team.members.size() == teamSize) finalNewTeams.add(new ArrayList<>(team.members));
            else remainingParticipants.addAll(team.members);
        }
        remainingParticipants.addAll(unassigned);
        return finalNewTeams;
    }

    private static TeamState findBestTeamForParticipant(List<TeamState> teamStates, Participant p, double overallAvg, int teamSize) {
        if (p == null) return null;
        String pRole = safeRole(p);
        List<TeamState> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        try {
            for (TeamState team : teamStates) {
                // Read operations are safe without synchronization
                if (team.members.size() >= teamSize) continue;
                if (countGame(team.members, p.getPreferredGame()) >= GAME_CAP) continue;
                if (pRole.equals("thinker") && team.roleCounts.getOrDefault("thinker", 0) >= MAX_THINKERS) continue;

                Map<String, Integer> roles = team.roleCounts;
                int uniqueRoles = roles.size();
                boolean addsNewRole = !roles.containsKey(pRole);
                if (!addsNewRole && uniqueRoles >= MIN_UNIQUE_ROLES && (pRole.equals("leader") || pRole.equals("thinker"))) continue;

                double newAvg = (double) (team.skillTotal + safeSkill(p)) / (team.members.size() + 1);
                double diff = Math.abs(newAvg - overallAvg);

                // Find the team(s) with the minimum difference
                if (diff < minDiff) {
                    validTeams.clear();
                    validTeams.add(team);
                    minDiff = diff;
                } else if (diff == minDiff) validTeams.add(team);
            }
        } catch (Exception ignored) {}

        if (!validTeams.isEmpty()) return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        return null;
    }

    private static TeamState findBestLeftoverTeam(List<TeamState> teamStates, Participant p, double overallAvg, int teamSize) {
        if (p == null) return null;
        List<TeamState> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        try {
            for (TeamState team : teamStates) {
                if (team.members.size() >= teamSize) continue;
                if (countGame(team.members, p.getPreferredGame()) >= GAME_CAP) continue;
                if (safeRole(p).equals("thinker") && team.roleCounts.getOrDefault("thinker", 0) >= MAX_THINKERS) continue;

                double newAvg = (double) (team.skillTotal + safeSkill(p)) / (team.members.size() + 1);
                double diff = Math.abs(newAvg - overallAvg);

                if (diff < minDiff) {
                    validTeams.clear();
                    validTeams.add(team);
                    minDiff = diff;
                } else if (diff == minDiff) validTeams.add(team);
            }
        } catch (Exception ignored) {}

        if (!validTeams.isEmpty()) return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        return null;
    }

    private static int countGame(List<Participant> team, String game) {
        if (game == null) return 0;
        int count = 0;
        for (Participant p : team)
            if (p.getPreferredGame() != null && p.getPreferredGame().equalsIgnoreCase(game)) count++;
        return count;
    }

    public static List<Participant> getRemainingParticipants() {
        return new ArrayList<>(remainingParticipants);
    }

    private static String safeRole(Participant p) {
        if (p == null) return "unknown";
        String r = p.getPersonalityType();
        return (r == null) ? "unknown" : r.toLowerCase();
    }

    private static int safeSkill(Participant p) {
        if (p == null) return 0;
        try {
            return p.getSkillLevel();
        } catch (Exception ignored) {
            return 0;
        }
    }
}