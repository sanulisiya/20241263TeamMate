package service;

import model.Participant;
import model.PersonalityType;
import model.RoleType;
import utility.LoggerService;
import exception.FileOperationException;
import exception.ParticipantValidationException;

import javax.swing.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                if (firstLine != null && (firstLine.toLowerCase().contains("id") || firstLine.toLowerCase().contains("name") || firstLine.toLowerCase().contains("email"))) {
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
            throw new FileOperationException(
                    "Error reading participant file: " + e.getMessage(),
                    filePath,
                    "READ",
                    e
            );
        } catch (Exception e) {
            throw new FileOperationException(
                    "Unexpected error while loading participants",
                    filePath,
                    "READ",
                    e
            );
        }

        LoggerService.logFileOperation("LOAD", filePath, "Loaded " + participants.size() + " participants");
        return participants;
    }

    // ---------------- PARSE PARTICIPANT (FOR TEAM OUTPUT FILE) ----------------

    private static Participant parseParticipant(String line) {
        String trimmedLine = line.trim();

        // Ignore empty lines, separator lines, and summary lines
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("----") || trimmedLine.startsWith("Summary") || trimmedLine.startsWith("Team Number")) {
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
                Participant participant = new Participant(id, name, email, game, skillLevel, preferredRole, personalityScore, personalityType);

                // Set team number if available
                if (!teamNumber.isEmpty()) {
                    participant.setTeamNumber(teamNumber);
                }

                return participant;
            }

        } catch (NumberFormatException e) {
            throw new ParticipantValidationException(
                    "Invalid number format in CSV row: " + trimmedLine,
                    "CSV_ROW",
                    trimmedLine,
                    e
            );
        } catch (IllegalArgumentException e) {
            throw new ParticipantValidationException(
                    "Invalid enum value in CSV row: " + trimmedLine,
                    "CSV_ROW",
                    trimmedLine,
                    e
            );
        } catch (Exception e) {
            throw new ParticipantValidationException(
                    "Invalid CSV row format: " + trimmedLine,
                    "CSV_ROW",
                    trimmedLine,
                    e
            );
        }

        throw new ParticipantValidationException(
                "Not enough columns in CSV row: " + trimmedLine,
                "CSV_ROW",
                trimmedLine
        );
    }

    public static String createNewCSV() {
        // Get Desktop path dynamically
        String desktopPath = System.getProperty("user.home") + "/Desktop";

        // Generate filename with timestamp
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = dtf.format(LocalDateTime.now());
        String fileName = "participants_" + timestamp + ".csv";

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write CSV header
            writer.append("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityType,PersonalityScore\n");
            System.out.println("CSV file created successfully: " + fileName);
        } catch (IOException e) {
            throw new FileOperationException(
                    "Error creating CSV file: " + e.getMessage(),
                    fileName,
                    "CREATE",
                    e
            );
        }

        return fileName; // Return the path of the newly created CSV
    }

    public static void main(String[] args) {
        // Create CSV file
        String newCSV = createNewCSV();

        // You can now pass this file path to your FileHandler to save participants
        System.out.println("You can now save participants to: " + newCSV);
    }

    // ---------------- SAVE PARTICIPANT ----------------

    public static void saveParticipant(String filePath, Participant p) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(filePath, true); // true for append mode

            String line = String.join(",", p.getId(), p.getName(), p.getEmail(), p.getPreferredGame(), String.valueOf(p.getSkillLevel()), p.getPreferredRole().name(), String.valueOf(p.getPersonalityScore()), p.getPersonalityType().name(), p.getTeamNumber() != null ? p.getTeamNumber() : "");
            writer.write(line + "\n");
            System.out.println("Participant saved to: " + filePath);

        } catch (IOException e) {
            throw new FileOperationException(
                    "Error saving participant to file: " + e.getMessage(),
                    filePath,
                    "SAVE",
                    e
            );
        } finally {
            // Ensure writer is always closed
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println("Error closing file: " + e.getMessage());
                }
            }
        }
        LoggerService.logFileOperation("SAVE", filePath, "Saved participant: " + p.getId());
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
            throw new FileOperationException(
                    "Error reading team file: " + e.getMessage(),
                    filePath,
                    "READ",
                    e
            );
        } catch (Exception e) {
            throw new FileOperationException(
                    "Unexpected error while loading teams",
                    filePath,
                    "READ",
                    e
            );
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

                Participant participant = new Participant(id, name, email, game, skillLevel, preferredRole, personalityScore, personalityType);
                participant.setTeamNumber(teamNumber);

                return participant;
            }

        } catch (Exception e) {
            throw new ParticipantValidationException(
                    "Invalid team participant row: " + trimmedLine,
                    "TEAM_ROW",
                    trimmedLine,
                    e
            );
        }

        throw new ParticipantValidationException(
                "Not enough columns in team participant row: " + trimmedLine,
                "TEAM_ROW",
                trimmedLine
        );
    }

    public static void ensureCSVExists(String filePath) {
        File file = new File(filePath);

        try {
            // Create parent folder if missing
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Create CSV file if it doesn't exist
            if (!file.exists()) {
                FileWriter writer = new FileWriter(file);
                writer.write("ID,Name,Email,Game,Skill,Role,PersonalityScore,PersonalityType\n");
                writer.close();
                System.out.println("CSV created at: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            throw new FileOperationException(
                    "Could not create CSV file: " + e.getMessage(),
                    filePath,
                    "CREATE",
                    e
            );
        }
    }

    public static String chooseLocationAndCreateCSV() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select folder to save participant_data.csv");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showSaveDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {

                File folder = chooser.getSelectedFile();
                String filePath = folder.getAbsolutePath() + File.separator + "participant_data.csv";

                FileWriter writer = new FileWriter(filePath);
                writer.write("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityType\n");
                writer.close();

                return filePath;  // return path of new CSV file
            }

        } catch (Exception e) {
            throw new FileOperationException(
                    "Error creating CSV file via file chooser: " + e.getMessage(),
                    "user_selected_path",
                    "CREATE",
                    e
            );
        }

        return null;
    }
}