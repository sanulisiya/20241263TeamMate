package service;

import model.Participant;
import model.PersonalityType;
import model.RoleType;
import utility.LoggerService;
import exception.FileOperationException;
import exception.ParticipantValidationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    // Logger instance
    private static final LoggerService logger = LoggerService.getInstance();

    // ---------------- SINGLE-THREADED LOADER ----------------

    public static List<Participant> loadParticipantsSingleThread(String filePath) {
        List<Participant> participants = new ArrayList<>();

//        // Validate file existence and readability first
//        validateFile(filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            int successCount = 0;
            int errorCount = 0;

            // Check if file has header and skip it
            if (br.ready()) {
                String firstLine = br.readLine();
                lineNumber++;

                if (firstLine != null) {
                    if (isHeaderLine(firstLine)) {
                        logger.debug("Skipping header line: " + firstLine);
                    } else {
                        // This might be data, so parse it
                        try {
                            Participant p = parseParticipant(firstLine, lineNumber);
                            if (p != null) {
                                participants.add(p);
                                successCount++;
                            }
                        } catch (Exception e) {
                            errorCount++;
                            logger.warn("Failed to parse line " + lineNumber + ": " + firstLine + " - " + e.getMessage());
                        }
                    }
                }
            }

            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    Participant p = parseParticipant(line, lineNumber);
                    if (p != null) {
                        participants.add(p);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.warn("Failed to parse line " + lineNumber + ": " + line + " - " + e.getMessage());
                    // Continue processing other lines instead of failing completely
                }
            }

            logger.info("Loaded " + successCount + " participants from: " + filePath +
                    " (Failed: " + errorCount + " lines)");

            if (successCount == 0 && errorCount > 0) {
                throw new FileOperationException(
                        "No valid participants found in file. Check file format.",
                        filePath,
                        "READ"
                );
            }

        } catch (FileNotFoundException e) {
            throw new FileOperationException(
                    "File not found: " + filePath,
                    filePath,
                    "READ",
                    e
            );
        } catch (IOException e) {
            throw new FileOperationException(
                    "Error reading file: " + e.getMessage(),
                    filePath,
                    "READ",
                    e
            );
        } catch (FileOperationException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            throw new FileOperationException(
                    "Unexpected error while loading participants: " + e.getMessage(),
                    filePath,
                    "READ",
                    e
            );
        }

        return participants;
    }

    // ---------------- FILE VALIDATION ----------------

    private static void validateFile(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileOperationException(
                    "File does not exist: " + filePath,
                    filePath,
                    "READ"
            );
        }

        if (!file.isFile()) {
            throw new FileOperationException(
                    "Path is not a file: " + filePath,
                    filePath,
                    "READ"
            );
        }

        if (!file.canRead()) {
            throw new FileOperationException(
                    "Cannot read file (permission denied): " + filePath,
                    filePath,
                    "READ"
            );
        }

        if (file.length() == 0) {
            throw new FileOperationException(
                    "File is empty: " + filePath,
                    filePath,
                    "READ"
            );
        }

        logger.debug("File validation passed: " + filePath);
    }

    private static boolean isHeaderLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        String lowerLine = line.toLowerCase();
        return lowerLine.contains("id") ||
                lowerLine.contains("name") ||
                lowerLine.contains("email") ||
                lowerLine.contains("game") ||
                lowerLine.contains("skill") ||
                lowerLine.contains("role") ||
                lowerLine.contains("personality");
    }

    // ---------------- PARSE PARTICIPANT (IMPROVED ERROR HANDLING) ----------------

    private static Participant parseParticipant(String line, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String trimmedLine = line.trim();

        // Ignore separator lines and summary lines
        if (trimmedLine.startsWith("----") || trimmedLine.startsWith("Summary") ||
                trimmedLine.startsWith("===") || trimmedLine.startsWith("***")) {
            return null;
        }

        String[] data = trimmedLine.split(",");

        // Clean up each field
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i].trim();
            // Remove quotes if present
            if (data[i].startsWith("\"") && data[i].endsWith("\"")) {
                data[i] = data[i].substring(1, data[i].length() - 1);
            }
        }

        try {
            // Handle different CSV formats
            if (data.length >= 8) {
                String id, name, email, game, teamNumber = "";
                int skillLevel, personalityScore;
                RoleType preferredRole;
                PersonalityType personalityType;

                // Determine the format based on first column
                if (data[0].matches("\\d+")) {
                    // Team output format: TeamNumber,ID,Name,Email,Game,Skill,Role,Score,PersonalityType
                    if (data.length < 9) {
                        throw new ParticipantValidationException(
                                "Team format requires 9 columns, found: " + data.length,
                                "CSV_ROW",
                                trimmedLine
                        );
                    }
                    teamNumber = data[0];
                    id = data[1];
                    name = data[2];
                    email = data[3];
                    game = data[4];
                    skillLevel = parseIntegerSafe(data[5], "SkillLevel", lineNumber);
                    preferredRole = parseRoleTypeSafe(data[6], lineNumber);
                    personalityScore = parseIntegerSafe(data[7], "PersonalityScore", lineNumber);
                    personalityType = parsePersonalityTypeSafe(data[8], lineNumber);
                } else {
                    // Main participants format: ID,Name,Email,Game,Skill,Role,Score,PersonalityType
                    id = data[0];
                    name = data[1];
                    email = data[2];
                    game = data[3];
                    skillLevel = parseIntegerSafe(data[4], "SkillLevel", lineNumber);
                    preferredRole = parseRoleTypeSafe(data[5], lineNumber);
                    personalityScore = parseIntegerSafe(data[6], "PersonalityScore", lineNumber);
                    personalityType = parsePersonalityTypeSafe(data[7], lineNumber);
                }

                // Validate required fields
                validateRequiredField(id, "ID", lineNumber);
                validateRequiredField(name, "Name", lineNumber);
                validateRequiredField(email, "Email", lineNumber);

                // Create participant
                Participant participant = new Participant(id, name, email, game, skillLevel, preferredRole, personalityScore, personalityType);

                // Set team number if available
                if (!teamNumber.isEmpty()) {
                    participant.setTeamNumber(teamNumber);
                }

                return participant;
            } else {
                throw new ParticipantValidationException(
                        "Not enough columns. Expected at least 8, found: " + data.length,
                        "CSV_ROW",
                        trimmedLine
                );
            }

        } catch (ParticipantValidationException e) {
            // Re-throw our custom exceptions with line number context
            throw new ParticipantValidationException(
                    "Line " + lineNumber + ": " + e.getMessage(),
                    e.getFieldName(),
                    e.getInvalidValue(),
                    e
            );
        } catch (Exception e) {
            throw new ParticipantValidationException(
                    "Line " + lineNumber + ": Unexpected parsing error - " + e.getMessage(),
                    "CSV_ROW",
                    trimmedLine,
                    e
            );
        }
    }

    // ---------------- SAFE PARSING METHODS ----------------

    private static int parseIntegerSafe(String value, String fieldName, int lineNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParticipantValidationException(
                    fieldName + " cannot be empty",
                    fieldName,
                    value
            );
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ParticipantValidationException(
                    "Invalid number format for " + fieldName + ": " + value,
                    fieldName,
                    value,
                    e
            );
        }
    }

    private static RoleType parseRoleTypeSafe(String value, int lineNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParticipantValidationException(
                    "Role cannot be empty",
                    "Role",
                    value
            );
        }

        try {
            return RoleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParticipantValidationException(
                    "Invalid role: " + value + ". Valid roles: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR",
                    "Role",
                    value,
                    e
            );
        }
    }

    private static PersonalityType parsePersonalityTypeSafe(String value, int lineNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParticipantValidationException(
                    "PersonalityType cannot be empty",
                    "PersonalityType",
                    value
            );
        }

        try {
            return PersonalityType.fromString(value);
        } catch (Exception e) {
            throw new ParticipantValidationException(
                    "Invalid personality type: " + value + ". Valid types: LEADER, THINKER, BALANCED, MOTIVATOR",
                    "PersonalityType",
                    value,
                    e
            );
        }
    }

    private static void validateRequiredField(String value, String fieldName, int lineNumber) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParticipantValidationException(
                    fieldName + " cannot be empty",
                    fieldName,
                    value
            );
        }
    }



    public static List<Participant> loadTeamsFromOutput(String filePath) {
        List<Participant> teamParticipants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                try {
                    Participant p = parseTeamParticipant(line);
                    if (p != null) teamParticipants.add(p);
                } catch (Exception e) {
                    logger.warn("Failed to parse team participant: " + line + " - " + e.getMessage());
                    // Continue with other lines
                }
            }

        } catch (IOException e) {
            throw new FileOperationException(
                    "Error reading team file: " + e.getMessage(),
                    filePath,
                    "READ",
                    e
            );
        }

        logger.info("Loaded " + teamParticipants.size() + " team participants from: " + filePath);
        return teamParticipants;
    }

    private static Participant parseTeamParticipant(String line) {
        // Simplified version for team files - uses the main parser
        return parseParticipant(line, 0); // lineNumber 0 for unknown
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
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("ID,Name,Email,Game,Skill,Role,PersonalityScore,PersonalityType\n");
                    logger.info("CSV created at: " + file.getAbsolutePath());
                }
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

}