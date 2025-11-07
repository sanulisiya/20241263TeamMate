package Service;

import model.Participant;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    // Load  participants from CSV file
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
                        data[0].trim(),                // ID
                        data[1].trim(),                // Name
                        data[2].trim(),                // Email
                        data[3].trim(),                // Preferred Game
                        Integer.parseInt(data[4].trim()), // Skill Level
                        data[5].trim(),                // Preferred Role
                        Integer.parseInt(data[6].trim()), // Personality Score
                        data[7].trim()                 // Personality Type
                );

                participants.add(p);
            }

        } catch (IOException e) {
            System.err.println(" Error reading file: " + e.getMessage());
        }

        return participants;
    }

    //  Append new participant to CSV file
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
}
