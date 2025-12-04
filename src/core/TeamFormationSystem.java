package core;

import model.Participant;
import java.util.List;

public interface TeamFormationSystem {

    // ==================== PARTICIPANT MANAGEMENT ====================
    // Creates a new participant through user input
    Participant createParticipant();
    //Validates if a participant has all required fields
    boolean validateParticipant(Participant participant);
    // Loads participants from a CSV file
    List<Participant> loadParticipants(String filePath);

    // Loads participants from a formatted teams output CSV
    List<Participant> loadTeamsOutput(String filePath);

    //Saves participants to a CSV file
    void saveParticipants(List<Participant> participants, String filePath);

    // ==================== TEAM OPERATIONS ====================

    // Forms balanced teams from participants
    List<List<Participant>> formTeams(List<Participant> participants, int teamSize);

    //  Forms teams from leftover/unassigned participants
    List<List<Participant>> formLeftoverTeams(int teamSize);
    List<Participant> getRemainingParticipants();
    void saveTeams(List<List<Participant>> teams, String filePath);

    // ==================== DATA MERGING ====================

    //  Merges new participants with organizer file
    List<Participant> mergeParticipants(String organizerFilePath, String outputPath);
    void addNewParticipant(Participant participant);
    int getNewParticipantsCount();

    List<Participant> getNewParticipants();
    void clearNewParticipants();

    // ==================== SURVEY & ANALYSIS ====================

    // Conducts personality survey through user input
    int conductPersonalitySurvey();
    String classifyPersonality(int score);
    double calculateCompatibility(Participant p1, Participant p2);

    // Validates if a file exists and is readable
    boolean validateFile(String filePath);
    void ensureCSVExists(String filePath);

    // ==================== SEARCH OPERATIONS ====================

    Participant findParticipantById(String id, List<Participant> participants);
    List<List<Participant>> formTeamsWithAlgorithm(List<Participant> participants, int teamSize);
}