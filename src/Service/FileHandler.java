package Service;

import model.Participant;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    // Load participants from original CSV
    public static List<Participant> loadParticipants(String filePath) {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) { // skip header row
                    isHeader = false;
                    continue;
                }

                String[] data = line.split(",");
                if (data.length < 8) continue; // skip invalid lines

                Participant p = new Participant(
                        data[0].trim(),
                        data[1].trim(),
                        data[2].trim(),
                        data[3].trim(),
                        Integer.parseInt(data[4].trim()),
                        data[5].trim(),
                        Integer.parseInt(data[6].trim()),
                        data[7].trim()
                );
                participants.add(p);
            }

        } catch (IOException e) {
            System.err.println(" Error reading file: " + e.getMessage());
        }

        return participants;
    }

    // Append new participant
    public static void saveParticipant(String filePath, Participant participant) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(
                    participant.getId() + "," +
                            participant.getName() + "," +
                            participant.getEmail() + "," +
                            participant.getPreferredGame() + "," +
                            participant.getSkillLevel() + "," +
                            participant.getPreferredRole() + "," +
                            participant.getPersonalityScore() + "," +
                            participant.getPersonalityType() + "\n"
            );
        } catch (IOException e) {
            System.err.println(" Error writing to file: " + e.getMessage());
        }
    }

    // 🆕 Load formatted team data with team numbers
    public static List<List<Participant>> loadFormattedTeams(String filePath) {
        List<List<Participant>> teams = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            // Team grouping map
            java.util.Map<Integer, List<Participant>> teamMap = new java.util.HashMap<>();

            while ((line = br.readLine()) != null) {
                if (isHeader) { // skip header if exists
                    isHeader = false;
                    if (line.toLowerCase().contains("team")) continue;
                }

                String[] data = line.split(",");
                if (data.length < 9) continue; // skip invalid rows

                // Column layout:
                // 0=TeamNumber, 1=ID, 2=Name, 3=Email, 4=Game, 5=Skill, 6=Role, 7=Score, 8=Type

                int teamNumber;
                try {
                    teamNumber = Integer.parseInt(data[0].trim());
                } catch (NumberFormatException e) {
                    continue; // skip bad rows
                }

                Participant p = new Participant(
                        data[1].trim(),  // ID
                        data[2].trim(),  // Name
                        data[3].trim(),  // Email
                        data[4].trim(),  // Preferred Game
                        Integer.parseInt(data[5].trim()), // Skill Level
                        data[6].trim(),  // Preferred Role
                        Integer.parseInt(data[7].trim()), // Personality Score
                        data[8].trim()   // Personality Type
                );

                // store team number
                try {
                    var teamField = Participant.class.getDeclaredField("teamId");
                    teamField.setAccessible(true);
                    teamField.setInt(p, teamNumber);
                } catch (Exception ignored) {}

                // add to team
                teamMap.computeIfAbsent(teamNumber, k -> new ArrayList<>()).add(p);
            }

            // Convert map to list
            teams.addAll(teamMap.values());

        } catch (IOException e) {
            System.err.println("Error reading formatted team file: " + e.getMessage());
        }

        return teams;
    }

}
