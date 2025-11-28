package cli;

import model.Participant;
import java.util.List;

public class TeamFormationResult {
    private final List<List<Participant>> teams;
    private final List<Participant> remainingPool;
    private final String updatedFilePath;

    public TeamFormationResult(List<List<Participant>> teams, List<Participant> remainingPool, String updatedFilePath) {
        this.teams = teams;
        this.remainingPool = remainingPool;
        this.updatedFilePath = updatedFilePath;
    }

    // Getters
    public List<List<Participant>> getTeams() { return teams; }
    public List<Participant> getRemainingPool() { return remainingPool; }
    public String getUpdatedFilePath() { return updatedFilePath; }
}