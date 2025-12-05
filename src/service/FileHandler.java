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
    private static String currentFilePath; // Track file path for better error context

    // ---------------- SINGLE-THREADED LOADER ----------------
    //3.(SD- upload csv)

    //04..(SD- View all Participant) .07.(SD-Team Formation)
    public static List<Participant> loadParticipantsSingleThread(String filePath) {
        currentFilePath = filePath; // Set current file path for error context  //01.(SD- View all Participant)
        List<Participant> participants = new ArrayList<>();

        try {
            validateFile(filePath);  //3.1.(SD- upload csv)

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
                        if (isHeaderLine(firstLine)) { //3.2.(SD- upload csv)
                            logger.debug("Skipping header line: " + firstLine);
                        } else {

                            try {
                                Participant p = parseParticipant(firstLine, lineNumber, filePath); //3.3.(SD- upload csv)
                                if (p != null) {
                                    participants.add(p);
                                    successCount++;
                                }
                            } catch (ParticipantValidationException e) { //3.4.(SD- upload csv)
                                errorCount++;
                                logger.warn("Failed to parse line " + lineNumber + ": " + firstLine +
                                        " - Field: " + e.getFieldName() +
                                        " - Error: " + e.getMessage());

                            } catch (Exception e) {
                                errorCount++;
                                logger.warn("Unexpected error parsing line " + lineNumber + ": " + e.getMessage());
                            }
                        }
                    }
                }

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    try {
                        Participant p = parseParticipant(line, lineNumber, filePath);
                        if (p != null) {
                            participants.add(p);
                            successCount++;
                        }
                    } catch (ParticipantValidationException e) {
                        errorCount++;
                        logger.warn("Failed to parse line " + lineNumber + ": " + line +
                                " - Field: " + e.getFieldName() +
                                " - Error: " + e.getMessage());
                        // Continue processing other lines
                    } catch (Exception e) {
                        errorCount++;
                        logger.warn("Unexpected error parsing line " + lineNumber + ": " + e.getMessage());
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
        } finally {
            currentFilePath = null; // Clear current file path
        }

        return participants;   //7.1.(SD-Team Formation)
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

    private static Participant parseParticipant(String line, int lineNumber, String filePath) {
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
                    skillLevel = parseIntegerSafe(data[5], "SkillLevel", lineNumber, filePath);
                    preferredRole = parseRoleTypeSafe(data[6], lineNumber, filePath);
                    personalityScore = parseIntegerSafe(data[7], "PersonalityScore", lineNumber, filePath);
                    personalityType = parsePersonalityTypeSafe(data[8], lineNumber, filePath);
                } else {
                    // Main participants format: ID,Name,Email,Game,Skill,Role,Score,PersonalityType
                    id = data[0];
                    name = data[1];
                    email = data[2];
                    game = data[3];
                    skillLevel = parseIntegerSafe(data[4], "SkillLevel", lineNumber, filePath);
                    preferredRole = parseRoleTypeSafe(data[5], lineNumber, filePath);
                    personalityScore = parseIntegerSafe(data[6], "PersonalityScore", lineNumber, filePath);
                    personalityType = parsePersonalityTypeSafe(data[7], lineNumber, filePath);
                }

                // Validate required fields
                validateRequiredField(id, "ID", lineNumber, filePath);
                validateRequiredField(name, "Name", lineNumber, filePath);
                validateRequiredField(email, "Email", lineNumber, filePath);

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
            // Re-throw with file context
            throw new ParticipantValidationException(
                    "File: " + filePath + ", Line " + lineNumber + ": " + e.getMessage(),
                    e.getFieldName(),
                    e.getInvalidValue(),
                    e
            );
        } catch (Exception e) {
            // Wrap in ParticipantValidationException for consistency
            throw new ParticipantValidationException(
                    "File: " + filePath + ", Line " + lineNumber + ": Unexpected parsing error - " + e.getMessage(),
                    "CSV_ROW",
                    trimmedLine,
                    e
            );
        }
    }

    // ---------------- SAFE PARSING METHODS ----------------

    private static int parseIntegerSafe(String value, String fieldName, int lineNumber, String filePath) {
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
                    "File: " + filePath + ", Line " + lineNumber +
                            ": Invalid number format for " + fieldName + ": '" + value + "'",
                    fieldName,
                    value,
                    e
            );
        }
    }

    private static RoleType parseRoleTypeSafe(String value, int lineNumber, String filePath) {
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
                    "File: " + filePath + ", Line " + lineNumber +
                            ": Invalid role: '" + value + "'. Valid roles: STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR",
                    "Role",
                    value,
                    e
            );
        }
    }

    private static PersonalityType parsePersonalityTypeSafe(String value, int lineNumber, String filePath) {
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
                    "File: " + filePath + ", Line " + lineNumber +
                            ": Invalid personality type: '" + value + "'. Valid types: LEADER, THINKER, BALANCED, MOTIVATOR",
                    "PersonalityType",
                    value,
                    e
            );
        }
    }

    private static void validateRequiredField(String value, String fieldName, int lineNumber, String filePath) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParticipantValidationException(
                    "File: " + filePath + ", Line " + lineNumber +
                            ": " + fieldName + " cannot be empty",
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
                    Participant p = parseTeamParticipant(line, filePath);
                    if (p != null) teamParticipants.add(p);
                } catch (ParticipantValidationException e) {
                    logger.warn("Failed to parse team participant: " + line +
                            " - Field: " + e.getFieldName() +
                            " - Error: " + e.getMessage());
                    // Continue with other lines
                } catch (Exception e) {
                    logger.warn("Unexpected error parsing team participant: " + e.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            throw new FileOperationException(
                    "Team file not found: " + filePath,
                    filePath,
                    "READ",
                    e
            );
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

    private static Participant parseTeamParticipant(String line, String filePath) {
        // Simplified version for team files - uses the main parser with line number 0
        return parseParticipant(line, 0, filePath);
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

        } catch (IOException e) {
            throw new FileOperationException(
                    "Could not create CSV file: " + e.getMessage(),
                    filePath,
                    "CREATE",
                    e
            );
        } catch (Exception e) {
            throw new FileOperationException(
                    "Unexpected error creating CSV: " + e.getMessage(),
                    filePath,
                    "CREATE",
                    e
            );
        }
    }
}