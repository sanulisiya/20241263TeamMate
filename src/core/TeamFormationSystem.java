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


    //Gets participants not assigned to any team

    List<Participant> getRemainingParticipants();


    //Saves formed teams to a CSV file

    void saveTeams(List<List<Participant>> teams, String filePath);


    // ==================== DATA MERGING ====================


    //  Merges new participants with organizer file

    List<Participant> mergeParticipants(String organizerFilePath, String outputPath);


    // Adds a new participant to merge pool

    void addNewParticipant(Participant participant);


    //Gets count of new participants waiting to merge

    int getNewParticipantsCount();


    // Gets list of new participants waiting to merge

    List<Participant> getNewParticipants();


    // Clears the new participants merge pool

    void clearNewParticipants();


    // ==================== SURVEY & ANALYSIS ====================

    // Conducts personality survey through user input

    int conductPersonalitySurvey();


    // Classifies personality type based on score

    String classifyPersonality(int score);

    // Calculates compatibility between two participants

    double calculateCompatibility(Participant p1, Participant p2);


    // ==================== FILE OPERATIONS ====================


    // Validates if a file exists and is readable

    boolean validateFile(String filePath);


    //Ensures CSV file exists, creates if missing

    void ensureCSVExists(String filePath);


    // ==================== SEARCH OPERATIONS ====================
    //Finds a participant by ID in a list

    Participant findParticipantById(String id, List<Participant> participants);

    // Forms teams using a specific algorithm
    List<List<Participant>> formTeamsWithAlgorithm(List<Participant> participants, int teamSize);
}