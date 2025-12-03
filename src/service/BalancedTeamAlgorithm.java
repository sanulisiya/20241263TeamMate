package service;

import model.Participant;
import java.util.*;

public class BalancedTeamAlgorithm extends TeamFormationAlgorithm {

    @Override
    public List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        // Call inherited validation
        validateParticipants(participants, teamSize);

        // Create a copy and sort by skill (highest first)
        List<Participant> sorted = new ArrayList<>(participants);
        sorted.sort((p1, p2) -> Integer.compare(p2.getSkillLevel(), p1.getSkillLevel()));

        int teamCount = (int) Math.ceil((double) sorted.size() / teamSize);
        List<List<Participant>> teams = new ArrayList<>();

        // Initialize empty teams
        for (int i = 0; i < teamCount; i++) {
            teams.add(new ArrayList<>());
        }

        // Snake distribution for balanced teams
        for (int i = 0; i < sorted.size(); i++) {
            int teamIndex;
            if ((i / teamSize) % 2 == 0) {
                // Forward order
                teamIndex = i % teamCount;
            } else {
                // Reverse order
                teamIndex = teamCount - 1 - (i % teamCount);
            }
            teams.get(teamIndex).add(sorted.get(i));
        }

        return teams;
    }
}