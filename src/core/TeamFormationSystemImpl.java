package core;

import model.Participant;
import service.*;
import utility.LoggerService;

import java.io.File;
import java.util.List;

public class TeamFormationSystemImpl implements TeamFormationSystem {
    //Logger instance for logging events
    private final LoggerService logger = LoggerService.getInstance();

    // ==================== PARTICIPANT MANAGEMENT ====================

    @Override
    public Participant createParticipant() {
        return ParticipantCreator.createNewParticipant();
    }

    @Override
    public boolean validateParticipant(Participant participant) {
        return participant != null && participant.isValid();
    }

    @Override //2.(SD- upload csv) (SD- View all Participant)

    public List<Participant> loadParticipants(String filePath) {
        return FileHandler.loadParticipantsSingleThread(filePath); //2.1.(SD- upload csv)
    }

    @Override
    public List<Participant> loadTeamsOutput(String filePath) {
        return TeamFileHandler.loadTeamsFromOutput(filePath);
    }

    @Override
    public void saveParticipants(List<Participant> participants, String filePath) {
        CSVMerger.saveMergedParticipants(participants, filePath);
    }

    // ==================== TEAM OPERATIONS ====================

    @Override
    public List<List<Participant>> formTeams(List<Participant> participants, int teamSize) {
        return TeamBuilder.formTeams(participants, teamSize);
    }

    @Override
    public List<List<Participant>> formLeftoverTeams(int teamSize) {
        return TeamBuilder.formLeftoverTeams(teamSize);
    }

    @Override
    public List<Participant> getRemainingParticipants() {
        return TeamBuilder.getRemainingParticipants();
    }

    @Override
    public void saveTeams(List<List<Participant>> teams, String filePath) { //2.(SD-save Teams)
        TeamFileHandler.saveTeamsToCSV(teams, filePath);
    }

    // ==================== DATA MERGING ====================

    @Override
    public List<Participant> mergeParticipants(String organizerFilePath, String outputPath) {
        return CSVMerger.mergeNewParticipants(organizerFilePath, outputPath);
    }

    @Override
    public void addNewParticipant(Participant participant) {
        CSVMerger.addNewParticipant(participant);
    }

    @Override
    public int getNewParticipantsCount() {
        return CSVMerger.getNewParticipantsCount();
    }

    @Override
    public List<Participant> getNewParticipants() {
        return CSVMerger.getNewParticipants();
    }

    @Override
    public void clearNewParticipants() {
        CSVMerger.clearNewParticipants();
    }

    // ==================== SURVEY & ANALYSIS ====================

    @Override
    public int conductPersonalitySurvey() {
        return Survey.conductPersonalitySurvey();
    }

    @Override
    public String classifyPersonality(int score) {
        return Survey.classifyPersonality(score);
    }

    @Override
    public double calculateCompatibility(Participant p1, Participant p2) {
        if (p1 == null || p2 == null) return 0.0;
        return p1.calculateCompatibility(p2);
    }

    // ==================== FILE OPERATIONS ====================

    @Override
    public boolean validateFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return false;
        try {
            File file = new File(filePath);
            return file.exists() && file.isFile() && file.canRead();
        } catch (Exception e) {
            logger.error("File validation error: " + filePath, e);
            return false;
        }
    }

    @Override
    public void ensureCSVExists(String filePath) {
        FileHandler.ensureCSVExists(filePath);
    }

    // ==================== SEARCH OPERATIONS ====================

    @Override
    public Participant findParticipantById(String id, List<Participant> participants) {
        if (participants == null || id == null) return null;
        return participants.stream()
                .filter(p -> p.getId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<List<Participant>> formTeamsWithAlgorithm(List<Participant> participants, int teamSize) {
        TeamFormationAlgorithm algorithm = new BalancedTeamAlgorithm();
        return algorithm.formTeams(participants, teamSize);
    }
}