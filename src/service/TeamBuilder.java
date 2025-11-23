package service;

import model.Participant;
import model.PersonalityType;
import model.RoleType;
import model.Team;

import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    private static final int MAX_GAME_PER_TEAM = 2;
    private static final int REQUIRED_THINKERS_MIN = 1;
    private static final int REQUIRED_THINKERS_MAX = 2;

    private static List<Participant> remainingParticipants = new ArrayList<>();

    // Main method to form teams
    public static List<List<Participant>> formTeams(List<Participant> originalList, int teamSize) {
        remainingParticipants.clear();

        if (originalList == null || originalList.isEmpty() || teamSize <= 1) {
            return Collections.emptyList();
        }

        int numTeams = originalList.size() / teamSize;
        if (numTeams == 0) {
            remainingParticipants.addAll(originalList);
            return Collections.emptyList();
        }

        List<Participant> pool = new ArrayList<>(originalList);
        List<Team> teams = initializeTeams(numTeams);

        // Sort pool: Leaders > Thinkers > Balanced > Motivators
        sortPoolByPriority(pool);

        // Assign Leaders
        assignLeaders(pool, teams, teamSize);

        // Assign Thinkers
        assignThinkers(pool, teams, teamSize);

        // Fill remaining slots with other participants
        assignRemainingParticipants(pool, teams, teamSize);

        // Remaining participants (who couldn't fit)
        remainingParticipants.addAll(pool.stream()
                .filter(p -> p.getTeamNumber() == null || p.getTeamNumber().isEmpty())
                .collect(Collectors.toList()));

        // Convert to List<List<Participant>> for CSV saving
        return teams.stream()
                .filter(team -> team.size() == teamSize)
                .map(Team::getMembers)
                .collect(Collectors.toList());
    }

    // -------------------- Helper Methods --------------------

    private static List<Team> initializeTeams(int numTeams) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= numTeams; i++) {
            teams.add(new Team(i));
        }
        return teams;
    }

    private static void sortPoolByPriority(List<Participant> pool) {
        pool.sort(Comparator
                .<Participant>comparingInt(p -> {
                    switch (p.getPersonalityType()) {
                        case LEADER: return 4;
                        case THINKER: return 3;
                        case BALANCED: return 2;
                        default: return 1; // MOTIVATOR
                    }
                })
                .thenComparingInt(Participant::getSkillLevel)
                .reversed()
        );
    }

    private static void assignLeaders(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> leaders = pool.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.LEADER && (p.getTeamNumber() == null || p.getTeamNumber().isEmpty()))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(leaders.size(), teams.size()); i++) {
            Participant leader = leaders.get(i);
            Team team = teams.get(i);
            if (team.size() < teamSize) {
                team.addMember(leader);
                leader.setTeamNumber(String.valueOf(team.getTeamId()));
            }
        }
    }

    private static void assignThinkers(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> thinkers = pool.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.THINKER && (p.getTeamNumber() == null || p.getTeamNumber().isEmpty()))
                .collect(Collectors.toList());

        for (Participant thinker : thinkers) {
            Team bestTeam = findTeamForThinker(teams, thinker, teamSize);
            if (bestTeam != null) {
                bestTeam.addMember(thinker);
                thinker.setTeamNumber(String.valueOf(bestTeam.getTeamId()));
            }
        }
    }

    private static Team findTeamForThinker(List<Team> teams, Participant thinker, int teamSize) {
        Team bestTeam = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Team team : teams) {
            if (team.size() >= teamSize) continue;

            long currentThinkers = team.getMembers().stream()
                    .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                    .count();

            if (currentThinkers >= REQUIRED_THINKERS_MAX) continue;

            double score = currentThinkers == 0 ? 1000 : 500; // prioritize teams without thinkers
            if (score > bestScore) {
                bestScore = score;
                bestTeam = team;
            }
        }
        return bestTeam;
    }

    private static void assignRemainingParticipants(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> unassigned = pool.stream()
                .filter(p -> p.getTeamNumber() == null || p.getTeamNumber().isEmpty())
                .sorted(Comparator.comparingInt(Participant::getSkillLevel).reversed())
                .collect(Collectors.toList());

        for (Participant participant : unassigned) {
            Team bestTeam = findBestTeamForParticipant(teams, participant, teamSize);
            if (bestTeam != null) {
                bestTeam.addMember(participant);
                participant.setTeamNumber(String.valueOf(bestTeam.getTeamId()));
            }
        }

        // Remove assigned from pool
        pool.removeIf(p -> p.getTeamNumber() != null && !p.getTeamNumber().isEmpty());
    }

    private static Team findBestTeamForParticipant(List<Team> teams, Participant participant, int teamSize) {
        for (Team team : teams) {
            if (team.size() >= teamSize) continue;

            if (team.countGame(participant.getPreferredGame()) >= MAX_GAME_PER_TEAM) continue;

            Set<RoleType> roles = new HashSet<>();
            team.getMembers().forEach(p -> roles.add(p.getPreferredRole()));
            roles.add(participant.getPreferredRole());
            if (roles.size() < minRolesRequired(teamSize)) continue;

            // Personality constraints
            if (participant.getPersonalityType() == PersonalityType.LEADER) {
                boolean hasLeader = team.getMembers().stream()
                        .anyMatch(p -> p.getPersonalityType() == PersonalityType.LEADER);
                if (hasLeader) continue;
            }
            if (participant.getPersonalityType() == PersonalityType.THINKER) {
                long currentThinkers = team.getMembers().stream()
                        .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                        .count();
                if (currentThinkers >= REQUIRED_THINKERS_MAX) continue;
            }

            return team;
        }
        return null;
    }

    private static int minRolesRequired(int teamSize) {
        return teamSize > 5 ? 4 : 3;
    }

    // ------------------- Remaining Participants -------------------

    public static List<Participant> getRemainingParticipants() {
        return new ArrayList<>(remainingParticipants);
    }

    public static void clearRemainingParticipants() {
        remainingParticipants.clear();
    }

    // ------------------- Debug / Stats -------------------

    public static void printFormationStats(List<List<Participant>> teams, List<Participant> allParticipants) {
        System.out.println("\nTeam Formation Summary:");
        for (int i = 0; i < teams.size(); i++) {
            List<Participant> team = teams.get(i);
            double avgSkill = team.stream().mapToInt(Participant::getSkillLevel).average().orElse(0);
            long leaders = team.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
            long thinkers = team.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
            long balanced = team.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();
            long motivators = team.stream().filter(p -> p.getPersonalityType() == PersonalityType.MOTIVATOR).count();

            System.out.printf("Team %d: Avg Skill %.2f, Leaders %d, Thinkers %d, Balanced %d, Motivators %d, Size %d\n",
                    i + 1, avgSkill, leaders, thinkers, balanced, motivators, team.size());
        }

        if (!remainingParticipants.isEmpty()) {
            System.out.println("\nRemaining Participants:");
            remainingParticipants.forEach(p -> System.out.println("  " + p));
        }

    }


}
