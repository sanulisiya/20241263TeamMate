package service;

import model.Participant;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TeamFileHandler {

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

            System.out.println("\n✔ Teams successfully saved to CSV!");
            System.out.println("Location: " + outputPath);
            System.out.println("Total formatted teams written: " + teams.size());

        } catch (IOException e) {
            System.err.println(" Error writing to CSV file: " + e.getMessage());
        }
    }
}
