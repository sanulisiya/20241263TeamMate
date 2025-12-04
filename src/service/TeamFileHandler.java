package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TeamFileHandler {

    private static final String CSV_DELIMITER = ",";

    public static void saveTeamsToCSV(List<List<Participant>> teams, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {

            // Write CSV header
            writer.write("Team Number,ID,Name,Email,Preferred Game,Skill Level,Preferred Role,Personality Score,Personality Type\n");

            // Loop through each team
            for (int i = 0; i < teams.size(); i++) {
                int teamNumber = i + 1;
                List<Participant> team = teams.get(i);

                // Write each participant in that team
                for (Participant p : team) {
                    writer.write(teamNumber + "," +
                            p.getId() + "," +
                            p.getName() + "," +
                            p.getEmail() + "," +
                            p.getPreferredGame() + "," +
                            p.getSkillLevel() + "," +
                            p.getPreferredRole() + "," +
                            p.getPersonalityScore() + "," +
                            p.getPersonalityType() + "\n");
                }

                // Add blank line after each team for readability
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println(" Error writing to CSV file: " + e.getMessage());
        }
    }


    //Reads the final team output CSV file and loads all participants,

    public static List<Participant> loadTeamsFromOutput(String filePath) {
        List<Participant> participants = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Skip header line
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(CSV_DELIMITER);


                if (parts.length >= 9) {
                    try {
                        String teamNumber = parts[0].trim();
                        String id = parts[1].trim();
                        String name = parts[2].trim();
                        String email = parts[3].trim();
                        String game = parts[4].trim();
                        int skillLevel = Integer.parseInt(parts[5].trim());

                        // Parse RoleType (Uppercase to match Enum constants)
                        String roleString = parts[6].trim().toUpperCase();
                        RoleType preferredRole = RoleType.valueOf(roleString);

                        int score = Integer.parseInt(parts[7].trim());

                        // Parse PersonalityType
                        String typeString = parts[8].trim().toUpperCase();
                        PersonalityType type = PersonalityType.valueOf(typeString);

                        Participant p = new Participant(id, name, email, game, skillLevel, preferredRole, score, type);

                        // Set the team number
                        p.setTeamNumber(teamNumber);

                        participants.add(p);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping malformed participant line: " + line + " Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading teams output file: " + e.getMessage());
            return new ArrayList<>();
        }
        return participants;
    }
}