package Service;

import model.Participant;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    private static final List<Participant> remainingParticipants =
            Collections.synchronizedList(new ArrayList<>());

    private static final int GAME_CAP = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MIN_UNIQUE_ROLES = 3;

    // ============================================================
    // TEAM STATE (THREAD-SAFE)
    // ============================================================
    private static class TeamState {

        final List<Participant> members = new ArrayList<>();
        final Map<String, Integer> roleCounts = new HashMap<>();
        int skillTotal = 0;
        final int teamId;

        public TeamState(int id) {
            this.teamId = id;
        }

        public void addMember(Participant p) {
            if (p == null) return;
            members.add(p);
            skillTotal += safeSkill(p);

            String role = safeRole(p);
            roleCounts.merge(role, 1, Integer::sum);
        }
    }

    // ============================================================
    // MAIN TEAM FORMATION (MULTITHREADED)
    // ============================================================
    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {

        remainingParticipants.clear();
        if (participants == null || participants.isEmpty() || teamSize <= 0)
            return Collections.emptyList();

        Map<String, List<Participant>> rolesMap =
                participants.stream().collect(Collectors.groupingBy(TeamBuilder::safeRole));

        List<Participant> leaders = new ArrayList<>(rolesMap.getOrDefault("leader", new ArrayList<>()));
        List<Participant> thinkers = new ArrayList<>(rolesMap.getOrDefault("thinker", new ArrayList<>()));
        List<Participant> balanced = new ArrayList<>(rolesMap.getOrDefault("balanced", new ArrayList<>()));
        List<Participant> motivators = new ArrayList<>(rolesMap.getOrDefault("motivator", new ArrayList<>()));

        List<Participant> remainingOthers = new ArrayList<>();
        remainingOthers.addAll(balanced);
        remainingOthers.addAll(motivators);

        Collections.shuffle(remainingOthers, new Random());
        Collections.shuffle(leaders, new Random());

        int possibleTeams = Math.min(leaders.size(), participants.size() / teamSize);
        if (possibleTeams == 0) {
            remainingParticipants.addAll(participants);
            return Collections.emptyList();
        }

        double overallAvg = participants.stream().mapToInt(TeamBuilder::safeSkill).average().orElse(0);

        List<TeamState> teamStates = new ArrayList<>();
        for (int i = 0; i < possibleTeams; i++) {
            TeamState state = new TeamState(i);
            state.addMember(leaders.get(i));
            teamStates.add(state);
        }

        if (leaders.size() > possibleTeams)
            remainingParticipants.addAll(leaders.subList(possibleTeams, leaders.size()));

        // Assign one thinker per team
        Collections.shuffle(thinkers, new Random());
        Iterator<Participant> thinkerIterator = thinkers.iterator();
        for (TeamState team : teamStates) {
            if (!thinkerIterator.hasNext()) break;
            Participant th = thinkerIterator.next();

            if (team.members.size() < teamSize &&
                    countGame(team.members, th.getPreferredGame()) < GAME_CAP) {
                team.addMember(th);
            } else {
                remainingOthers.add(0, th);
            }
            thinkerIterator.remove();
        }

        remainingOthers.addAll(thinkers);

        // ============================================================
        // MULTITHREADED ASSIGNMENT USING THREAD POOL
        // ============================================================

        ExecutorService executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );

        List<Future<?>> futures = new ArrayList<>();

        for (Participant p : remainingOthers) {
            futures.add(executor.submit(() -> {

                TeamState bestTeam = findBestTeamForParticipant(teamStates, p, overallAvg, teamSize);

                if (bestTeam != null) {
                    synchronized (bestTeam) {
                        if (bestTeam.members.size() < teamSize) {
                            bestTeam.addMember(p);
                        } else {
                            synchronized (remainingParticipants) {
                                remainingParticipants.add(p);
                            }
                        }
                    }
                } else {
                    synchronized (remainingParticipants) {
                        remainingParticipants.add(p);
                    }
                }
            }));
        }

        // Wait for all tasks
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }

        executor.shutdown();

        // ============================================================
        // BUILD FINAL TEAMS
        // ============================================================
        List<List<Participant>> finalTeams = new ArrayList<>();

        for (TeamState team : teamStates) {
            if (team.members.size() == teamSize)
                finalTeams.add(new ArrayList<>(team.members));
            else
                remainingParticipants.addAll(team.members);
        }

        return finalTeams;
    }

    // ============================================================
    // LEFTOVER TEAM FORMATION (SEQUENTIAL - OPTIONAL TO PARALLELIZE)
    // ============================================================
    public static List<List<Participant>> formLeftoverTeams(int teamSize) {

        List<Participant> pool = new ArrayList<>(getRemainingParticipants());
        if (pool.size() < teamSize) return Collections.emptyList();

        double avg = pool.stream().mapToInt(TeamBuilder::safeSkill).average().orElse(0);

        int maxTeams = pool.size() / teamSize;
        List<TeamState> teams = new ArrayList<>();

        pool.sort(Comparator.comparingInt(TeamBuilder::safeSkill).reversed());
        List<Participant> temp = new ArrayList<>(pool);
        pool.clear();

        for (int i = 0; i < maxTeams; i++) {
            if (temp.isEmpty()) break;

            TeamState t = new TeamState(i + 100);
            t.addMember(temp.remove(0));
            teams.add(t);
        }

        pool.addAll(temp);
        Collections.shuffle(pool);

        List<Participant> unassigned = new ArrayList<>();

        for (Participant p : pool) {
            TeamState t = findBestLeftoverTeam(teams, p, avg, teamSize);
            if (t != null) t.addMember(p);
            else unassigned.add(p);
        }

        List<List<Participant>> finalTeams = new ArrayList<>();
        remainingParticipants.clear();

        for (TeamState t : teams) {
            if (t.members.size() == teamSize)
                finalTeams.add(new ArrayList<>(t.members));
            else
                remainingParticipants.addAll(t.members);
        }

        remainingParticipants.addAll(unassigned);

        return finalTeams;
    }

    // ============================================================
    // TEAM SELECTION HELPERS
    // ============================================================
    private static TeamState findBestTeamForParticipant(List<TeamState> teams, Participant p,
                                                        double avg, int teamSize) {

        if (p == null) return null;

        List<TeamState> candidates = new ArrayList<>();
        double bestDiff = Double.MAX_VALUE;

        String pRole = safeRole(p);

        for (TeamState t : teams) {
            if (t.members.size() >= teamSize) continue;
            if (countGame(t.members, p.getPreferredGame()) >= GAME_CAP) continue;

            if (pRole.equals("thinker") && t.roleCounts.getOrDefault("thinker", 0) >= MAX_THINKERS)
                continue;

            boolean addsNewRole = !t.roleCounts.containsKey(pRole);
            if (!addsNewRole && t.roleCounts.size() >= MIN_UNIQUE_ROLES &&
                    (pRole.equals("leader") || pRole.equals("thinker")))
                continue;

            double newAvg = (double) (t.skillTotal + safeSkill(p)) / (t.members.size() + 1);
            double diff = Math.abs(newAvg - avg);

            if (diff < bestDiff) {
                candidates.clear();
                candidates.add(t);
                bestDiff = diff;
            } else if (diff == bestDiff) {
                candidates.add(t);
            }
        }

        if (candidates.isEmpty()) return null;

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static TeamState findBestLeftoverTeam(List<TeamState> teams, Participant p,
                                                  double avg, int teamSize) {

        if (p == null) return null;

        List<TeamState> candidates = new ArrayList<>();
        double best = Double.MAX_VALUE;

        for (TeamState t : teams) {
            if (t.members.size() >= teamSize) continue;
            if (countGame(t.members, p.getPreferredGame()) >= GAME_CAP) continue;

            double newAvg = (double) (t.skillTotal + safeSkill(p)) / (t.members.size() + 1);
            double diff = Math.abs(newAvg - avg);

            if (diff < best) {
                candidates.clear();
                candidates.add(t);
                best = diff;
            } else if (diff == best) {
                candidates.add(t);
            }
        }

        if (candidates.isEmpty()) return null;

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static int countGame(List<Participant> members, String game) {
        if (game == null) return 0;

        int count = 0;
        for (Participant p : members)
            if (game.equalsIgnoreCase(p.getPreferredGame()))
                count++;

        return count;
    }

    // ============================================================
    // UTILITY HELPERS
    // ============================================================
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
        } catch (Exception e) {
            return 0;
        }
    }
}
