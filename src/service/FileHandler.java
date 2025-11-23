package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    // ---------------- NORMAL SINGLE-THREADED LOADER ----------------

    public static List<Participant> loadParticipantsSingleThread(String filePath) {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Check if file has header and skip it
            if (br.ready()) {
                String firstLine = br.readLine();
                // Only skip if it's actually a header (contains typical header keywords)
                if (firstLine != null && (firstLine.toLowerCase().contains("id") ||
                        firstLine.toLowerCase().contains("name") ||
                        firstLine.toLowerCase().contains("email"))) {
                    // This is a header, we've already read it, so continue
                } else {
                    // This might be data, so parse it
                    Participant p = parseParticipant(firstLine);
                    if (p != null) participants.add(p);
                }
            }

            while ((line = br.readLine()) != null) {
                Participant p = parseParticipant(line);
                if (p != null) participants.add(p);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return participants;
    }

    // ---------------- PARSE PARTICIPANT (FOR TEAM OUTPUT FILE) ----------------

    private static Participant parseParticipant(String line) {
        String trimmedLine = line.trim();

        // Ignore empty lines, separator lines, and summary lines
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("----") ||
                trimmedLine.startsWith("Summary") || trimmedLine.startsWith("Team Number")) {
            return null;
        }

        String[] data = trimmedLine.split(",");

        try {
            // Handle different CSV formats:
            // Format 1: Main participants file (ID,Name,Email,Game,Skill,Role,Score,PersonalityType)
            // Format 2: Team output file (TeamNumber,ID,Name,Email,Game,Skill,Role,Score,PersonalityType)

            if (data.length >= 8) {
                String id, name, email, game, teamNumber = "";
                int skillLevel, personalityScore;
                RoleType preferredRole;
                PersonalityType personalityType;

                // Determine the format based on first column
                if (data[0].trim().matches("\\d+")) {
                    // This is team output format: TeamNumber,ID,Name,Email,Game,Skill,Role,Score,PersonalityType
                    teamNumber = data[0].trim();
                    id = data[1].trim();
                    name = data[2].trim();
                    email = data[3].trim();
                    game = data[4].trim();
                    skillLevel = Integer.parseInt(data[5].trim());
                    preferredRole = RoleType.valueOf(data[6].trim().toUpperCase());
                    personalityScore = Integer.parseInt(data[7].trim());
                    personalityType = PersonalityType.fromString(data[8].trim());
                } else {
                    // This is main participants format: ID,Name,Email,Game,Skill,Role,Score,PersonalityType
                    id = data[0].trim();
                    name = data[1].trim();
                    email = data[2].trim();
                    game = data[3].trim();
                    skillLevel = Integer.parseInt(data[4].trim());
                    preferredRole = RoleType.valueOf(data[5].trim().toUpperCase());
                    personalityScore = Integer.parseInt(data[6].trim());
                    personalityType = PersonalityType.fromString(data[7].trim());
                }

                // Create participant using the correct constructor
                Participant participant = new Participant(
                        id, name, email, game, skillLevel,
                        preferredRole, personalityScore, personalityType
                );

                // Set team number if available
                if (!teamNumber.isEmpty()) {
                    participant.setTeamNumber(teamNumber);
                }

                return participant;
            }

        } catch (NumberFormatException e) {
            System.err.println("Skipping invalid row (Invalid number format): " + trimmedLine);
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping invalid row (Invalid enum value): " + trimmedLine);
            return null;
        } catch (Exception e) {
            System.err.println("Skipping invalid row: " + trimmedLine + " Error: " + e.getMessage());
            return null;
        }

        System.err.println("Skipping invalid row (Not enough columns): " + trimmedLine);
        return null;
    }

    // ---------------- SAVE PARTICIPANT ----------------

    public static void saveParticipant(String filePath, Participant participant) {
        // Check if file exists and write header if it doesn't
        boolean fileExists = new File(filePath).exists();

        try (FileWriter writer = new FileWriter(filePath, true)) {
            // Write header if file is new
            if (!fileExists) {
                writer.write("ID,Name,Email,Preferred Game,Skill Level,Preferred Role,Personality Score,Personality Type\n");
            }

            writer.write(
                    participant.getId() + "," +
                            participant.getName() + "," +
                            participant.getEmail() + "," +
                            participant.getPreferredGame() + "," +
                            participant.getSkillLevel() + "," +
                            participant.getPreferredRole().name() + "," +
                            participant.getPersonalityScore() + "," +
                            participant.getPersonalityType().name() + "\n"
            );

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    // ---------------- LOAD TEAMS FROM OUTPUT FILE ----------------

    public static List<Participant> loadTeamsFromOutput(String filePath) {
        List<Participant> teamParticipants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                Participant p = parseTeamParticipant(line);
                if (p != null) teamParticipants.add(p);
            }

        } catch (IOException e) {
            System.err.println("Error reading team file: " + e.getMessage());
        }

        return teamParticipants;
    }

    private static Participant parseTeamParticipant(String line) {
        String trimmedLine = line.trim();

        if (trimmedLine.isEmpty() || trimmedLine.startsWith("----")) {
            return null;
        }

        String[] data = trimmedLine.split(",");

        try {
            if (data.length >= 9) {
                String teamNumber = data[0].trim();
                String id = data[1].trim();
                String name = data[2].trim();
                String email = data[3].trim();
                String game = data[4].trim();
                int skillLevel = Integer.parseInt(data[5].trim());
                RoleType preferredRole = RoleType.valueOf(data[6].trim().toUpperCase());
                int personalityScore = Integer.parseInt(data[7].trim());
                PersonalityType personalityType = PersonalityType.fromString(data[8].trim());

                Participant participant = new Participant(
                        id, name, email, game, skillLevel,
                        preferredRole, personalityScore, personalityType
                );
                participant.setTeamNumber(teamNumber);

                return participant;
            }

        } catch (Exception e) {
            System.err.println("Skipping invalid team row: " + trimmedLine + " Error: " + e.getMessage());
            return null;
        }

        return null;
    }
}