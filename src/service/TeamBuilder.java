package service;

import utility.LoggerService;
import model.Participant;
import model.Team; // Using the external model.Team class
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

    // Logger instance
    public static final LoggerService logger = LoggerService.getInstance();

    // *** REMOVED: private static class TeamState ***

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        logger.info("Starting team formation process");
        logger.debug("Parameters - Participants: " + participants.size() + ", TeamSize: " + teamSize);

        remainingParticipants.clear();

        // Validate input parameters with custom exception
        if (participants == null || participants.isEmpty() || teamSize <= 0) {
            throw new TeamFormationException(
                    "Invalid parameters for team formation: participants=" +
                            (participants != null ? participants.size() : "null") +
                            ", teamSize=" + teamSize,
                    "INVALID_PARAMETERS"
            );
        }

        try {
            // Group by safeRole, which mimics the logic now inside model.Team's addMember
            Map<String, List<Participant>> rolesMap = participants.stream()
                    .collect(Collectors.groupingBy(TeamBuilder::safeRole));

            // Log role distribution
            logger.debug("Role distribution - Leaders: " + rolesMap.getOrDefault("leader", new ArrayList<>()).size() +
                    ", Thinkers: " + rolesMap.getOrDefault("thinker", new ArrayList<>()).size() +
                    ", Balanced: " + rolesMap.getOrDefault("balanced", new ArrayList<>()).size() +
                    ", Motivators: " + rolesMap.getOrDefault("motivator", new ArrayList<>()).size());

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
                logger.warn("No possible teams formed - insufficient leaders or participants");
                remainingParticipants.addAll(participants);
                return Collections.emptyList();
            }

            logger.info("Possible teams to form: " + possibleTeams);

            double overallAvg = participants.stream()
                    .mapToInt(TeamBuilder::safeSkill)
                    .average()
                    .orElse(0);

            logger.debug("Overall average skill level: " + String.format("%.2f", overallAvg));

            // *** CHANGE: Using model.Team instead of TeamState ***
            List<Team> teams = new ArrayList<>();
            Collections.shuffle(leaders, new Random());
            for (int i = 0; i < possibleTeams; i++) {
                Team team = new Team(i);
                team.addMember(leaders.get(i));
                teams.add(team);
            }
            if (leaders.size() > possibleTeams) {
                logger.debug("Excess leaders added to remaining: " + (leaders.size() - possibleTeams));
                remainingParticipants.addAll(leaders.subList(possibleTeams, leaders.size()));
            }

            // Assign one thinker per team (sequential)
            Collections.shuffle(thinkers, new Random());
            Iterator<Participant> thinkerIterator = thinkers.iterator();
            int thinkersAssigned = 0;
            // *** CHANGE: Iterating over List<Team> ***
            for (Team team : teams) {
                if (!thinkerIterator.hasNext()) break;
                Participant thinker = thinkerIterator.next();
                // *** CHANGE: Using Team.getMembers().size() and Team.getGameCount() ***
                if (team.getMembers().size() < teamSize && team.getGameCount(thinker.getPreferredGame()) < GAME_CAP) {
                    team.addMember(thinker);
                    thinkerIterator.remove();
                    thinkersAssigned++;
                } else {
                    remainingOthers.add(0, thinker);
                    thinkerIterator.remove();
                }
            }
            remainingOthers.addAll(thinkers); // Add any unassigned thinkers back to the pool

            logger.debug("Thinkers assigned to teams: " + thinkersAssigned);
            logger.info("Starting multi-threaded team assignment with " + remainingOthers.size() + " participants");

            // --- Multi-threaded Assignment using Parallel Stream ---

            remainingOthers.parallelStream().forEach(p -> {
                // *** CHANGE: Passing List<Team> ***
                Team bestTeam = findBestTeamForParticipant(teams, p, overallAvg, teamSize);

                if (bestTeam != null) {
                    // Synchronize access to the specific team object being modified
                    synchronized (bestTeam) {
                        // Double-check condition inside the lock just in case another thread filled it
                        if (bestTeam.getMembers().size() < teamSize) {
                            bestTeam.addMember(p);
                            logger.debug("Assigned participant " + p.getId() + " to team " + bestTeam.getTeamId());
                        } else {
                            // If blocked and team is full, add to remaining list (needs synchronization)
                            remainingParticipants.add(p);
                            logger.debug("Team " + bestTeam.getTeamId() + " full, participant " + p.getId() + " added to remaining");
                        }
                    }
                } else {
                    // Synchronize access to the static remainingParticipants list
                    remainingParticipants.add(p);
                    logger.debug("No suitable team found for participant " + p.getId() + ", added to remaining");
                }
            });

            // --- End Multi-threaded Assignment ---

            List<List<Participant>> finalTeams = new ArrayList<>();
            int completeTeams = 0;
            // int incompleteParticipants = 0; // Removed unnecessary variable

            // Collect results sequentially
            // *** CHANGE: Iterating over List<Team> ***
            for (Team team : teams) {
                if (team.getMembers().size() == teamSize) {
                    finalTeams.add(new ArrayList<>(team.getMembers()));
                    completeTeams++;
                    logger.debug("Team " + team.getTeamId() + " completed with " + team.getMembers().size() + " members");
                } else {
                    remainingParticipants.addAll(team.getMembers());
                    // incompleteParticipants += team.getMembers().size(); // Removed
                    logger.debug("Team " + team.getTeamId() + " incomplete with " + team.getMembers().size() + " members, added to remaining");
                }
            }

            logger.info("Team formation completed - Teams: " + finalTeams.size() +
                    ", Complete teams: " + completeTeams +
                    ", Remaining participants: " + remainingParticipants.size()
            );

            return finalTeams;

        } catch (Exception e) {
            if (e instanceof TeamFormationException) {
                throw e; // Re-throw our custom exception
            }
            throw new TeamFormationException(
                    "Unexpected error during team formation: " + e.getMessage(),
                    "FORMATION_ERROR",
                    e
            );
        }
    }

    public static List<List<Participant>> formLeftoverTeams(int teamSize) {
        logger.info("Starting leftover team formation");

        List<Participant> pool = new ArrayList<>(getRemainingParticipants());
        if (teamSize <= 0 || pool.size() < teamSize) {
            logger.warn("Cannot form leftover teams - insufficient participants: " + pool.size());
            return Collections.emptyList();
        }

        logger.debug("Leftover pool size: " + pool.size() + ", Target team size: " + teamSize);

        try {
            double poolAvgSkill = pool.stream()
                    .mapToInt(TeamBuilder::safeSkill)
                    .average()
                    .orElse(0);

            int maxNewTeams = pool.size() / teamSize;
            // *** CHANGE: Using model.Team instead of TeamState ***
            List<Team> newTeams = new ArrayList<>();
            pool.sort(Comparator.comparingInt(TeamBuilder::safeSkill).reversed());
            List<Participant> tempPool = new ArrayList<>(pool);
            pool.clear();

            logger.debug("Creating " + maxNewTeams + " new teams from leftovers");

            for (int i = 0; i < maxNewTeams; i++) {
                if (tempPool.isEmpty()) break;
                // *** CHANGE: Using new Team(id) ***
                Team team = new Team(newTeams.size() + 100);
                team.addMember(tempPool.remove(0));
                newTeams.add(team);
                logger.debug("Created leftover team " + team.getTeamId() + " with initial member");
            }
            pool.addAll(tempPool);
            Collections.shuffle(pool, new Random());

            List<Participant> unassigned = new ArrayList<>();
            int assignedCount = 0;

            for (Participant p : pool) {
                // *** CHANGE: Passing List<Team> ***
                Team bestTeam = findBestLeftoverTeam(newTeams, p, poolAvgSkill, teamSize);
                if (bestTeam != null) {
                    bestTeam.addMember(p);
                    assignedCount++;
                    logger.debug("Assigned leftover participant " + p.getId() + " to team " + bestTeam.getTeamId());
                } else {
                    unassigned.add(p);
                    logger.debug("No suitable leftover team for participant " + p.getId());
                }
            }

            List<List<Participant>> finalNewTeams = new ArrayList<>();
            remainingParticipants.clear();
            int leftoverCompleteTeams = 0;

            // *** CHANGE: Iterating over List<Team> ***
            for (Team team : newTeams) {
                if (team.getMembers().size() == teamSize) {
                    finalNewTeams.add(new ArrayList<>(team.getMembers()));
                    leftoverCompleteTeams++;
                } else {
                    remainingParticipants.addAll(team.getMembers());
                }
            }
            remainingParticipants.addAll(unassigned);

            logger.info("Leftover team formation completed - New teams: " + finalNewTeams.size() +
                    ", Complete leftover teams: " + leftoverCompleteTeams +
                    ", Still unassigned: " + remainingParticipants.size()
            );

            return finalNewTeams;

        } catch (Exception e) {
            throw new TeamFormationException(
                    "Error during leftover team formation: " + e.getMessage(),
                    "LEFTOVER_FORMATION_ERROR",
                    e
            );
        }
    }

    // *** CHANGE: Method now accepts List<Team> ***
    private static Team findBestTeamForParticipant(List<Team> teams, Participant p, double overallAvg, int teamSize) {
        if (p == null) return null;

        String pRole = safeRole(p);
        String pGame = safeGame(p);
        List<Team> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        try {
            // *** CHANGE: Iterating over List<Team> ***
            for (Team team : teams) {
                // Read operations are safe without synchronization
                if (team.getMembers().size() >= teamSize) continue;

                // *** CHANGE: Using new Team methods ***
                if (team.getGameCount(pGame) >= GAME_CAP) continue;
                if (pRole.equals("thinker") && team.getRoleCount("thinker") >= MAX_THINKERS) continue;

                int uniqueRoles = team.getUniqueRoleCount();
                boolean addsNewRole = team.getRoleCount(pRole) == 0;

                if (!addsNewRole && uniqueRoles >= MIN_UNIQUE_ROLES && (pRole.equals("leader") || pRole.equals("thinker"))) continue;

                double newAvg = (double) (team.getTotalSkill() + safeSkill(p)) / (team.getMembers().size() + 1);
                double diff = Math.abs(newAvg - overallAvg);

                // Find the team(s) with the minimum difference
                if (diff < minDiff) {
                    validTeams.clear();
                    validTeams.add(team);
                    minDiff = diff;
                } else if (diff == minDiff) validTeams.add(team);
            }
        } catch (Exception e) {
            logger.error("Error finding best team for participant " + p.getId(), e);
        }

        if (!validTeams.isEmpty()) {
            logger.debug("Found " + validTeams.size() + " valid teams for participant " + p.getId());
            return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        }

        logger.debug("No valid teams found for participant " + p.getId());
        return null;
    }

    // *** CHANGE: Method now accepts List<Team> ***
    private static Team findBestLeftoverTeam(List<Team> teams, Participant p, double overallAvg, int teamSize) {
        if (p == null) return null;

        String pRole = safeRole(p);
        String pGame = safeGame(p);
        List<Team> validTeams = new ArrayList<>();
        double minDiff = Double.MAX_VALUE;

        try {
            // *** CHANGE: Iterating over List<Team> ***
            for (Team team : teams) {
                if (team.getMembers().size() >= teamSize) continue;

                // *** CHANGE: Using new Team methods ***
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
        } catch (Exception e) {
            logger.error("Error finding best leftover team for participant " + p.getId(), e);
        }

        if (!validTeams.isEmpty()) {
            return validTeams.get(ThreadLocalRandom.current().nextInt(validTeams.size()));
        }
        return null;
    }

    // *** REMOVED: private static int countGame(...) helper method ***

    public static List<Participant> getRemainingParticipants() {
        logger.debug("Retrieving remaining participants: " + remainingParticipants.size());
        return new ArrayList<>(remainingParticipants);
    }

    // Helper to safely get the lower-cased game string
    private static String safeGame(Participant p) {
        if (p == null || p.getPreferredGame() == null) return "unknown";
        return p.getPreferredGame().toLowerCase();
    }

    private static String safeRole(Participant p) {
        if (p == null) return "unknown";
        String r = String.valueOf(p.getPersonalityType());
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