package service;

import utility.LoggerService;
import model.Participant;
import model.Team;
import exception.TeamFormationException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

public class TeamBuilder {

    // NOTE: This list needs synchronization if accessed by multiple threads outside of the parallel stream
    private static final List<Participant> remainingParticipants = Collections.synchronizedList(new ArrayList<>());
    private static final int GAME_CAP = 2;
    private static final int MAX_THINKERS = 2;
    private static final int MIN_UNIQUE_ROLES = 3;

    public static final LoggerService logger = LoggerService.getInstance();

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        logger.info("Starting team formation process");
        remainingParticipants.clear();

        if (participants == null || participants.isEmpty() || teamSize <= 0) {
            throw new TeamFormationException("Invalid parameters", "INVALID_PARAMETERS");
        }

        try {
            Map<String, List<Participant>> rolesMap = participants.stream()
                    .collect(Collectors.groupingBy(TeamBuilder::safeRole));

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

            // 1. Seed Teams with Leaders
            List<Team> teams = new ArrayList<>();
            Collections.shuffle(leaders, new Random());
            for (int i = 0; i < possibleTeams; i++) {
                Team team = new Team(i);
                team.addMember(leaders.get(i));
                teams.add(team);
            }
            if (leaders.size() > possibleTeams) {
                remainingParticipants.addAll(leaders.subList(possibleTeams, leaders.size()));
            }

            // 2. Distribute Thinkers Sequentially (1 per team initially)
            Collections.shuffle(thinkers, new Random());
            Iterator<Participant> thinkerIterator = thinkers.iterator();

            for (Team team : teams) {
                if (!thinkerIterator.hasNext()) break;
                Participant thinker = thinkerIterator.next();
                if (team.getMembers().size() < teamSize && team.getGameCount(thinker.getPreferredGame()) < GAME_CAP) {
                    team.addMember(thinker);
                    thinkerIterator.remove();
                } else {
                    remainingOthers.add(0, thinker); // Move to general pool if can't fit
                    thinkerIterator.remove();
                }
            }
            remainingOthers.addAll(thinkers);

            // 3. Multi-threaded Greedy Assignment
            remainingOthers.parallelStream().forEach(p -> {
                Team bestTeam = findBestTeamForParticipant(teams, p, overallAvg, teamSize);

                if (bestTeam != null) {
                    synchronized (bestTeam) {
                        // *** CRITICAL FIX: DOUBLE-CHECK CONSTRAINTS ***
                        // We must verify limits again inside the lock because another thread
                        // might have filled the spot while we were waiting.

                        boolean sizeOk = bestTeam.getMembers().size() < teamSize;
                        boolean gameOk = bestTeam.getGameCount(p.getPreferredGame()) < GAME_CAP;

                        boolean roleOk = true;
                        if (safeRole(p).equals("thinker")) {
                            // Re-check thinker limit
                            roleOk = bestTeam.getRoleCount("thinker") < MAX_THINKERS;
                        }

                        if (sizeOk && gameOk && roleOk) {
                            bestTeam.addMember(p);
                        } else {
                            // If the spot was taken, put back in remaining
                            remainingParticipants.add(p);
                        }
                    }
                } else {
                    remainingParticipants.add(p);
                }
            });

            // 4. Finalize Teams
            List<List<Participant>> finalTeams = new ArrayList<>();
            for (Team team : teams) {
                if (team.getMembers().size() == teamSize) {
                    finalTeams.add(new ArrayList<>(team.getMembers()));
                } else {
                    remainingParticipants.addAll(team.getMembers());
                }
            }

            return finalTeams;

        } catch (Exception e) {
            if (e instanceof TeamFormationException) throw e;
            throw new TeamFormationException("Error forming teams", "FORMATION_ERROR", e);
        }
    }

    public static List<List<Participant>> formLeftoverTeams(int teamSize) {
        List<Participant> pool = new ArrayList<>(getRemainingParticipants());
        if (teamSize <= 0 || pool.size() < teamSize) return Collections.emptyList();

        double poolAvgSkill = pool.stream().mapToInt(TeamBuilder::safeSkill).average().orElse(0);
        int maxNewTeams = pool.size() / teamSize;

        List<Team> newTeams = new ArrayList<>();
        pool.sort(Comparator.comparingInt(TeamBuilder::safeSkill).reversed());

        // Seed leftovers
        for (int i = 0; i < maxNewTeams; i++) {
            Team team = new Team(newTeams.size() + 100);
            team.addMember(pool.remove(0));
            newTeams.add(team);
        }

        Collections.shuffle(pool);
        List<Participant> unassigned = new ArrayList<>();

        // Leftover assignment is sequential, so no race conditions here
        for (Participant p : pool) {
            Team bestTeam = findBestLeftoverTeam(newTeams, p, poolAvgSkill, teamSize);
            if (bestTeam != null) {
                bestTeam.addMember(p);
            } else {
                unassigned.add(p);
            }
        }

        List<List<Participant>> finalNewTeams = new ArrayList<>();
        remainingParticipants.clear();

        for (Team team : newTeams) {
            if (team.getMembers().size() == teamSize) {
                finalNewTeams.add(new ArrayList<>(team.getMembers()));
            } else {
                remainingParticipants.addAll(team.getMembers());
            }
        }
        remainingParticipants.addAll(unassigned);

        return finalNewTeams;
    }

    private static Team findBestTeamForParticipant(List<Team> teams, Participant p, double overallAvg, int teamSize) {
        if (p == null) return null;

        String pRole = safeRole(p);
        String pGame = safeGame(p);
        List<Team> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        // Note: This is a read-only check. It might be stale by the time we write!
        for (Team team : teams) {
            if (team.getMembers().size() >= teamSize) continue;
            if (team.getGameCount(pGame) >= GAME_CAP) continue;
            if (pRole.equals("thinker") && team.getRoleCount("thinker") >= MAX_THINKERS) continue;

            int uniqueRoles = team.getUniqueRoleCount();
            boolean addsNewRole = team.getRoleCount(pRole) == 0;
            if (!addsNewRole && uniqueRoles >= MIN_UNIQUE_ROLES && (pRole.equals("leader") || pRole.equals("thinker"))) continue;

            double newAvg = (double) (team.getTotalSkill() + safeSkill(p)) / (team.getMembers().size() + 1);
            double diff = Math.abs(newAvg - overallAvg);

            if (diff < minDiff) {
                validTeams.clear();
                validTeams.add(team);
                minDiff = diff;
            } else if (diff == minDiff) validTeams.add(team);
        }

        if (!validTeams.isEmpty()) {
            return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        }
        return null;
    }

    private static Team findBestLeftoverTeam(List<Team> teams, Participant p, double overallAvg, int teamSize) {
        // ... (Logic same as provided previously, simplified for brevity as it runs sequentially)
        // Ensure same logic as findBestTeam but with looser role rules
        if (p == null) return null;
        String pRole = safeRole(p);
        String pGame = safeGame(p);
        List<Team> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        for (Team team : teams) {
            if (team.getMembers().size() >= teamSize) continue;
            if (team.getGameCount(pGame) >= GAME_CAP) continue;
            if (pRole.equals("thinker") && team.getRoleCount("thinker") >= MAX_THINKERS) continue;

            double newAvg = (double) (team.getTotalSkill() + safeSkill(p)) / (team.getMembers().size() + 1);
            double diff = Math.abs(newAvg - overallAvg);

            if (diff < minDiff) {
                validTeams.clear();
                validTeams.add(team);
                minDiff = diff;
            } else if (diff == minDiff) validTeams.add(team);
        }

        if (!validTeams.isEmpty()) {
            return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        }
        return null;
    }

    public static List<Participant> getRemainingParticipants() {
        return new ArrayList<>(remainingParticipants);
    }

    private static String safeGame(Participant p) {
        return (p == null || p.getPreferredGame() == null) ? "unknown" : p.getPreferredGame().toLowerCase();
    }

    private static String safeRole(Participant p) {
        if (p == null) return "unknown";
        return (p.getPersonalityType() == null) ? "unknown" : p.getPersonalityType().toString().toLowerCase();
    }

    private static int safeSkill(Participant p) {
        return (p == null) ? 0 : p.getSkillLevel();
    }
}