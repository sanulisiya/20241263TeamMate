package service;

import model.Participant;
import model.PersonalityType;
import model.RoleType;
import model.Team;

import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    // --- CONSTRAINTS ---
    private static final int MAX_GAME_PER_TEAM = 2; // Game Variety: Max 2 from same game
    private static final int REQUIRED_LEADERS = 1; // Personality Mix: Must have 1 Leader
    private static final int REQUIRED_THINKERS_MIN = 1; // Personality Mix: Min 1 Thinker
    private static final int REQUIRED_THINKERS_MAX = 2; // Personality Mix: Max 2 Thinkers

    // Scoring factors
    private static final double GAME_PENALTY_FACTOR = 100.0;
    private static final double PERSONALITY_PENALTY_FACTOR = 50.0;
    private static final double SKILL_DEVIATION_WEIGHT = 30.0;
    private static final double ROLE_DIVERSITY_BONUS = 100.0;
    private static final double SKILL_BALANCE_WEIGHT = 25.0;

    private static List<Participant> remainingParticipants = new ArrayList<>();

    // --------------------------------------------------------------------------
    // Public API (Main Orchestration)
    // --------------------------------------------------------------------------

    public static List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty() || teamSize <= 1) {
            return Collections.emptyList();
        }

        int numTeams = participants.size() / teamSize;
        if (numTeams == 0) {
            remainingParticipants = new ArrayList<>(participants);
            return Collections.emptyList();
        }

        List<Participant> pool = new ArrayList<>(participants);
        List<Team> teams = initializeTeams(numTeams);

        // 1. Sort the pool by priority (Leaders > Thinkers > High Skill)
        sortPoolByPriority(pool);

        // 2. First pass: Assign leaders to ensure each team gets exactly one
        assignLeadersToTeams(pool, teams, teamSize);

        // 3. Second pass: Assign thinkers to ensure each team gets 1-2 thinkers
        assignThinkersToTeams(pool, teams, teamSize);

        // 4. Third pass: Assign remaining players with optimized scoring
        assignRemainingPlayers(pool, teams, teamSize);

        // 5. Collect remaining participants
        remainingParticipants = pool.stream()
                .filter(p -> p.getTeamNumber().isEmpty())
                .collect(Collectors.toList());

        // 6. Convert Team objects back to List<List<Participant>> format
        return teams.stream()
                .filter(team -> team.size() == teamSize)
                .map(Team::getMembers)
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------------------------
    // Core Logic Methods
    // --------------------------------------------------------------------------

    private static List<Team> initializeTeams(int numTeams) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= numTeams; i++) {
            teams.add(new Team(i));
        }
        return teams;
    }

    private static void sortPoolByPriority(List<Participant> pool) {
        // Priority: Leaders > Thinkers > High Skill
        pool.sort(Comparator
                .<Participant>comparingInt(p -> {
                    if (p.getPersonalityType() == PersonalityType.LEADER) return 3;
                    if (p.getPersonalityType() == PersonalityType.THINKER) return 2;
                    return 1;
                })
                .thenComparingInt(Participant::getSkillLevel)
                .reversed()
        );
    }

    private static void assignLeadersToTeams(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> leaders = pool.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.LEADER && p.getTeamNumber().isEmpty())
                .collect(Collectors.toList());

        // Assign exactly one leader to each team
        for (int i = 0; i < Math.min(leaders.size(), teams.size()); i++) {
            Participant leader = leaders.get(i);
            Team team = teams.get(i);
            if (team.size() < teamSize) {
                team.addMember(leader);
                leader.setTeamNumber(String.valueOf(team.getTeamId()));
            }
        }
    }

    private static void assignThinkersToTeams(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> thinkers = pool.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.THINKER && p.getTeamNumber().isEmpty())
                .collect(Collectors.toList());

        // Assign thinkers to teams that need them (1-2 per team)
        for (Participant thinker : thinkers) {
            if (thinker.getTeamNumber().isEmpty()) {
                Team bestTeam = findBestTeamForThinker(teams, thinker, teamSize);
                if (bestTeam != null) {
                    bestTeam.addMember(thinker);
                    thinker.setTeamNumber(String.valueOf(bestTeam.getTeamId()));
                }
            }
        }
    }

    private static Team findBestTeamForThinker(List<Team> teams, Participant thinker, int teamSize) {
        Team bestTeam = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Team team : teams) {
            if (team.size() < teamSize) {
                long currentThinkers = team.getMembers().stream()
                        .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                        .count();

                // Skip teams that already have max thinkers
                if (currentThinkers >= REQUIRED_THINKERS_MAX) {
                    continue;
                }

                double score = 0;

                // Priority: teams with no thinkers get highest priority
                if (currentThinkers == 0) {
                    score = 300.0;
                }
                // Then teams with one thinker
                else if (currentThinkers == 1) {
                    score = 100.0;
                }

                // Add skill balance consideration
                double currentAvgSkill = team.getAvgSkill();
                double newAvgSkill = (team.getTotalSkill() + thinker.getSkillLevel()) / (double) (team.size() + 1);
                double skillBalance = -Math.abs(newAvgSkill - currentAvgSkill) * 20.0;
                score += skillBalance;

                if (score > bestScore) {
                    bestScore = score;
                    bestTeam = team;
                }
            }
        }
        return bestTeam;
    }

    private static void assignRemainingPlayers(List<Participant> pool, List<Team> teams, int teamSize) {
        List<Participant> unassigned = pool.stream()
                .filter(p -> p.getTeamNumber() == null || p.getTeamNumber().isEmpty())
                .collect(Collectors.toList());

        // Sort remaining players by skill level (high to low) for better balance
        unassigned.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        for (Iterator<Participant> iterator = unassigned.iterator(); iterator.hasNext(); ) {
            Participant participant = iterator.next();

            Team bestFitTeam = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            // Evaluate all teams
            for (Team team : teams) {
                if (team.size() < teamSize) {
                    double score = evaluateAssignmentScore(team, participant, teams, teamSize);

                    if (score > bestScore) {
                        bestScore = score;
                        bestFitTeam = team;
                    }
                }
            }

            // If a valid team is found, assign the participant
            if (bestFitTeam != null && bestScore != Double.NEGATIVE_INFINITY) {
                bestFitTeam.addMember(participant);
                participant.setTeamNumber(String.valueOf(bestFitTeam.getTeamId()));
                iterator.remove();
            }
        }

        // Update the main pool - CORRECTED LINE
        pool.removeIf(p -> p.getTeamNumber() != null && !p.getTeamNumber().isEmpty());
        pool.addAll(unassigned);
    }

    /**
     * Calculates a fitness score for adding a participant to a team based on all soft constraints.
     */
    private static double evaluateAssignmentScore(Team team, Participant participant, List<Team> allTeams, int teamSize) {
        double score = 1000.0; // Base score

        // --- 1. Hard Constraints Check (Immediate rejection if violated) ---
        if (!isAssignmentValid(team, participant, teamSize)) {
            return Double.NEGATIVE_INFINITY;
        }

        // --- 2. Game Variety (Soft Penalty) ---
        int gameCount = Math.toIntExact(team.countGame(participant.getPreferredGame()));
        score -= gameCount * GAME_PENALTY_FACTOR;

        // --- 3. Role Diversity (Bonus) ---
        RoleType newRole = participant.getPreferredRole();
        if (team.getMembers().stream().noneMatch(p -> p.getPreferredRole().equals(newRole))) {
            score += ROLE_DIVERSITY_BONUS;
        }

        // --- 4. Personality Mix (Soft Penalty/Bonus) ---
        score += calculatePersonalityScore(team, participant, teamSize);

        // --- 5. Skill Balance (Penalty) ---
        double globalAvgSkill = allTeams.stream()
                .filter(t -> t.size() > 0)
                .mapToDouble(Team::getAvgSkill)
                .average()
                .orElse(5.0);

        double newAvgSkill = (team.getTotalSkill() + participant.getSkillLevel()) / (double) (team.size() + 1);
        double skillDeviationPenalty = Math.pow(newAvgSkill - globalAvgSkill, 2);
        score -= skillDeviationPenalty * SKILL_DEVIATION_WEIGHT;

        // --- 6. Team Completion Bonus ---
        if (team.size() == teamSize - 1) {
            score += 200.0;
        }

        // --- 7. Current Team Skill Balance ---
        if (team.size() > 0) {
            double currentAvg = team.getAvgSkill();
            double skillDiff = Math.abs(participant.getSkillLevel() - currentAvg);
            score -= skillDiff * 25.0;
        }

        return score;
    }

    /**
     * Checks all hard constraints for a potential assignment.
     */
    private static boolean isAssignmentValid(Team team, Participant participant, int teamSize) {
        // --- HARD CONSTRAINT 1: Game Variety Cap (Max 2 from same game) ---
        if (team.countGame(participant.getPreferredGame()) >= MAX_GAME_PER_TEAM) {
            return false;
        }

        // --- HARD CONSTRAINT 2: Personality Mix - Leader Check ---
        if (participant.getPersonalityType() == PersonalityType.LEADER) {
            boolean hasLeader = team.getMembers().stream()
                    .anyMatch(p -> p.getPersonalityType() == PersonalityType.LEADER);
            if (hasLeader) {
                return false; // Team already has a leader
            }
        }

        // --- HARD CONSTRAINT 3: Personality Mix - Thinker Check ---
        if (participant.getPersonalityType() == PersonalityType.THINKER) {
            long currentThinkers = team.getMembers().stream()
                    .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                    .count();
            if (currentThinkers >= REQUIRED_THINKERS_MAX) {
                return false; // Team already has max thinkers
            }
        }

        // --- HARD CONSTRAINT 4: Final Team Composition Check ---
        if (team.size() == teamSize - 1) {
            // Check if team will have exactly 1 leader after adding this participant
            Map<PersonalityType, Long> finalCounts = getPersonalityCounts(team, participant);
            long finalLeaders = finalCounts.getOrDefault(PersonalityType.LEADER, 0L);
            long finalThinkers = finalCounts.getOrDefault(PersonalityType.THINKER, 0L);

            if (finalLeaders != REQUIRED_LEADERS) {
                return false; // Team must have exactly 1 leader
            }

            if (finalThinkers < REQUIRED_THINKERS_MIN || finalThinkers > REQUIRED_THINKERS_MAX) {
                return false; // Team must have 1-2 thinkers
            }

            // --- HARD CONSTRAINT 5: Role Diversity ---
            int requiredRoles = getMinRolesRequired(teamSize);
            long distinctRoles = getDistinctRoles(team, participant);
            if (distinctRoles < requiredRoles) {
                return false;
            }
        }

        return true;
    }

    // --- Helper Methods ---

    private static int getMinRolesRequired(int teamSize) {
        return teamSize > 5 ? 4 : 3;
    }

    private static long getDistinctRoles(Team team, Participant potentialNewMember) {
        Set<RoleType> roles = team.getMembers().stream()
                .map(Participant::getPreferredRole)
                .collect(Collectors.toSet());
        if (potentialNewMember != null) {
            roles.add(potentialNewMember.getPreferredRole());
        }
        return roles.size();
    }

    private static double calculatePersonalityScore(Team team, Participant participant, int teamSize) {
        double score = 0;
        Map<PersonalityType, Long> counts = getPersonalityCounts(team, null);
        PersonalityType type = participant.getPersonalityType();

        switch (type) {
            case LEADER:
                // Only reachable if team needs a leader (due to hard constraint)
                if (counts.getOrDefault(PersonalityType.LEADER, 0L) == 0) {
                    score += 500.0;
                }
                break;

            case THINKER:
                long currentThinkers = counts.getOrDefault(PersonalityType.THINKER, 0L);
                if (currentThinkers == 0) {
                    score += 300.0; // High bonus for first thinker
                } else if (currentThinkers == 1) {
                    score += 100.0; // Moderate bonus for second thinker
                }
                break;

            case BALANCED:
            case MOTIVATOR:
                // Bonus for filling out the team
                score += 50.0;
                break;
        }

        return score;
    }

    private static Map<PersonalityType, Long> getPersonalityCounts(Team team, Participant potentialNewMember) {
        List<Participant> members = new ArrayList<>(team.getMembers());
        if (potentialNewMember != null) {
            members.add(potentialNewMember);
        }
        return members.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType, Collectors.counting()));
    }

    // --------------------------------------------------------------------------
    // Utility and Reporting
    // --------------------------------------------------------------------------

    public static List<Participant> getRemainingParticipants() {
        return new ArrayList<>(remainingParticipants);
    }

    public static void clearRemainingParticipants() {
        remainingParticipants.clear();
    }

    public static String safeRole(Participant p) {
        return p.getPreferredRole() != null ? p.getPreferredRole().name() : "UNKNOWN";
    }

    public static void printFormationStats(List<List<Participant>> teams, List<Participant> allParticipants) {
        if (teams.isEmpty()) {
            System.out.println("No teams were formed.");
            return;
        }

        System.out.println("\n================== FORMATION STATISTICS ==================");

        // Skill Balance Analysis
        List<Double> avgSkills = teams.stream()
                .map(team -> team.stream().mapToInt(Participant::getSkillLevel).average().orElse(0.0))
                .collect(Collectors.toList());

        double overallAvgSkill = allParticipants.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0.0);

        double teamAvgOfAvgs = avgSkills.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = avgSkills.stream()
                .mapToDouble(avg -> Math.pow(avg - teamAvgOfAvgs, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        System.out.println("✅ Skill Balance:");
        System.out.printf("   Overall Participant Avg Skill: %.2f\n", overallAvgSkill);
        System.out.printf("   Average Team Skill Deviation (Std Dev): %.2f\n", stdDev);

        for (int i = 0; i < teams.size(); i++) {
            System.out.printf("   Team %d Avg Skill: %.2f\n", (i + 1), avgSkills.get(i));
        }

        // Diversity Checks
        System.out.println("\n✅ Diversity Checks (Game/Role/Personality):");
        int teamSize = teams.get(0).size();
        int minRolesRequired = getMinRolesRequired(teamSize);

        boolean allConstraintsMet = true;

        for (int i = 0; i < teams.size(); i++) {
            List<Participant> team = teams.get(i);

            // Game Variety Check
            Map<String, Long> gameCounts = team.stream()
                    .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));
            long maxGameCount = gameCounts.values().stream().mapToLong(l -> l).max().orElse(0);

            // Role Diversity Check
            long distinctRoles = team.stream().map(Participant::getPreferredRole).distinct().count();

            // Personality Mix Check
            Map<PersonalityType, Long> personalityCounts = team.stream()
                    .collect(Collectors.groupingBy(Participant::getPersonalityType, Collectors.counting()));

            boolean gameOK = maxGameCount <= MAX_GAME_PER_TEAM;
            boolean rolesOK = distinctRoles >= minRolesRequired;
            boolean leaderOK = personalityCounts.getOrDefault(PersonalityType.LEADER, 0L) == REQUIRED_LEADERS;
            boolean thinkerOK = personalityCounts.getOrDefault(PersonalityType.THINKER, 0L) >= REQUIRED_THINKERS_MIN &&
                    personalityCounts.getOrDefault(PersonalityType.THINKER, 0L) <= REQUIRED_THINKERS_MAX;

            if (!(gameOK && rolesOK && leaderOK && thinkerOK)) {
                allConstraintsMet = false;
            }

            System.out.printf("   Team %d:\n", (i + 1));
            System.out.printf("     - Max Game Count: %d (Cap: %d) %s\n", maxGameCount, MAX_GAME_PER_TEAM, (gameOK ? "🟢" : "🔴"));
            System.out.printf("     - Distinct Roles: %d (Min: %d) %s\n", distinctRoles, minRolesRequired, (rolesOK ? "🟢" : "🔴"));
            System.out.printf("     - Personality Mix: Leader=%d (%s), Thinker=%d (%s)\n",
                    personalityCounts.getOrDefault(PersonalityType.LEADER, 0L),
                    (leaderOK ? "🟢" : "🔴"),
                    personalityCounts.getOrDefault(PersonalityType.THINKER, 0L),
                    (thinkerOK ? "🟢" : "🔴"));

            // Detailed personality breakdown
            System.out.printf("     - Personality Breakdown: %s\n", personalityCounts.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }

        System.out.printf("\n🎯 Overall Constraints Met: %s\n", allConstraintsMet ? "🟢 SUCCESS" : "🔴 FAILED");
        System.out.printf("📊 Remaining Unassigned Participants: %d\n", remainingParticipants.size());
    }
}