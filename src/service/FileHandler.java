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
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                Participant p = parseParticipant(line);
                if (p != null) participants.add(p);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return participants;
    }

    // ---------------- PARSE PARTICIPANT ----------------

    private static Participant parseParticipant(String line) {
        String[] data = line.split(",");
        if (data.length < 8) return null;

        try {
            RoleType preferredRole = RoleType.valueOf(data[5].trim().toUpperCase());
            PersonalityType personalityType = PersonalityType.valueOf(data[7].trim().toUpperCase());

            return new Participant(
                    data[0].trim(),
                    data[1].trim(),
                    data[2].trim(),
                    data[3].trim(),
                    Integer.parseInt(data[4].trim()),
                    preferredRole,
                    Integer.parseInt(data[6].trim()),
                    personalityType
            );

        } catch (Exception e) {
            System.err.println("Skipping invalid row: " + line);
            return null;
        }
    }

    // ---------------- SAVE PARTICIPANT ----------------

    public static void saveParticipant(String filePath, Participant participant) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
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
}
