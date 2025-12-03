package service;

import model.Participant;
import java.util.List;

public abstract class TeamFormationAlgorithm {
    // Abstract method - polymorphism
    public abstract List<List<Participant>> formTeams(List<Participant> participants, int teamSize);

    // Concrete method - inheritance
    protected void validateParticipants(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be empty");
        }
        if (teamSize <= 0) {
            throw new IllegalArgumentException("Team size must be positive");
        }
    }


}