// File: service/CSVMerger.java
package service;

import model.Participant;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CSVMerger {

    /**
     * Merges two CSV files, removing duplicates based on email address
     * and generating new IDs for participant-added users
     */
    public static List<Participant> mergeCSVFiles(String organizerFilePath, String participantFilePath, String outputPath) {
        List<Participant> allParticipants = new ArrayList<>();
        Set<String> emailSet = new HashSet<>(); // To track unique emails

        try {
            // Load participants from organizer file first (this is our base)
            List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
            System.out.println("Loaded " + organizerParticipants.size() + " participants from organizer file: " + organizerFilePath);

            // Find the highest ID from organizer file to continue numbering
            int highestId = findHighestParticipantId(organizerParticipants);
            System.out.println("Highest ID found in organizer file: " + highestId);

            // Load participants from participant-added file
            List<Participant> participantParticipants = FileHandler.loadParticipantsSingleThread(participantFilePath);
            System.out.println("Loaded " + participantParticipants.size() + " participants from participant file: " + participantFilePath);

            // First, add all organizer participants (these keep their original IDs)
            for (Participant p : organizerParticipants) {
                allParticipants.add(p);
                emailSet.add(p.getEmail().toLowerCase().trim());
            }

            // Then, add participant-added participants with new generated IDs
            int newParticipantsAdded = 0;
            for (Participant p : participantParticipants) {
                String email = p.getEmail().toLowerCase().trim();

                if (!emailSet.contains(email)) {
                    // Generate new ID for this participant-added user
                    String newId = generateNewParticipantId(highestId);
                    Participant newParticipant = createParticipantWithNewId(p, newId);
                    allParticipants.add(newParticipant);
                    emailSet.add(email);
                    highestId++; // Increment for next new participant
                    newParticipantsAdded++;

                    System.out.println("Assigned new ID " + newId + " to: " + p.getName() + " (" + email + ")");
                } else {
                    System.out.println("Skipping duplicate participant with email: " + email);
                }
            }

            // Save merged list to output file
            saveMergedParticipants(allParticipants, outputPath);

            System.out.println("\n✓ Merge completed successfully!");
            System.out.println("Total unique participants: " + allParticipants.size());
            System.out.println("New participants added: " + newParticipantsAdded);
            System.out.println("Merged file saved to: " + outputPath);

        } catch (Exception e) {
            System.out.println("Error merging CSV files: " + e.getMessage());
            e.printStackTrace();
        }

        return allParticipants;
    }

    /**
     * Find the highest numeric ID from organizer participants
     * Assumes IDs are in format like "P001", "P002", etc.
     */
    private static int findHighestParticipantId(List<Participant> participants) {
        int highest = 0;

        for (Participant p : participants) {
            try {
                String id = p.getId().trim();
                // Extract numeric part from ID (assuming format like P001, P123, etc.)
                if (id.matches("[Pp]\\d+")) {
                    int numericId = Integer.parseInt(id.substring(1));
                    if (numericId > highest) {
                        highest = numericId;
                    }
                }
            } catch (Exception e) {
                // Skip if ID format is unexpected
                System.out.println("Warning: Unexpected ID format for participant: " + p.getId());
            }
        }

        return highest;
    }

    /**
     * Generate new participant ID based on the highest existing ID
     */
    private static String generateNewParticipantId(int highestId) {
        int newIdNumber = highestId + 1;
        return String.format("P%03d", newIdNumber); // Format as P001, P002, etc.
    }

    /**
     * Create a new participant with the generated ID
     */
    private static Participant createParticipantWithNewId(Participant original, String newId) {
        return new Participant(newId, original.getName(), original.getEmail(), original.getPreferredGame(), original.getSkillLevel(), original.getPreferredRole(), original.getPersonalityScore(), original.getPersonalityType(), original.getTeamNumber());
    }

    /**
     * Save merged participants to a new CSV file
     */
    private static void saveMergedParticipants(List<Participant> participants, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            // Write CSV header
            writer.write("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType,TeamNumber\n");

            // Write each participant
            for (Participant p : participants) {
                String line = String.join(",", p.getId(), p.getName(), p.getEmail(), p.getPreferredGame(), String.valueOf(p.getSkillLevel()), p.getPreferredRole().name(), String.valueOf(p.getPersonalityScore()), p.getPersonalityType().name(), p.getTeamNumber() != null ? p.getTeamNumber() : "");
                writer.write(line + "\n");
            }

        } catch (IOException e) {
            System.out.println("Error saving merged CSV: " + e.getMessage());
            throw new RuntimeException("Failed to save merged CSV file", e);
        }
    }


    /**
     * Quick merge without saving to file - just returns merged list
     */
    public static List<Participant> quickMerge(String organizerFilePath, String participantFilePath) {
        List<Participant> allParticipants = new ArrayList<>();
        Set<String> emailSet = new HashSet<>();

        try {
            List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
            List<Participant> participantParticipants = FileHandler.loadParticipantsSingleThread(participantFilePath);

            // Find highest ID
            int highestId = findHighestParticipantId(organizerParticipants);

            // Add organizer participants first
            for (Participant p : organizerParticipants) {
                allParticipants.add(p);
                emailSet.add(p.getEmail().toLowerCase().trim());
            }

            // Add participant-added participants with new IDs
            for (Participant p : participantParticipants) {
                String email = p.getEmail().toLowerCase().trim();

                if (!emailSet.contains(email)) {
                    String newId = generateNewParticipantId(highestId);
                    Participant newParticipant = createParticipantWithNewId(p, newId);
                    allParticipants.add(newParticipant);
                    emailSet.add(email);
                    highestId++;
                }
            }

        } catch (Exception e) {
            System.out.println("Error in quick merge: " + e.getMessage());
        }

        return allParticipants;
    }


    /**
     * Alternative method that preserves original participant IDs if they follow the format
     * and only generates new IDs for those that don't
     */
    public static List<Participant> smartMerge(String organizerFilePath, String participantFilePath, String outputPath) {
        List<Participant> allParticipants = new ArrayList<>();
        Set<String> emailSet = new HashSet<>();
        Set<String> usedIds = new HashSet<>();

        try {
            // Load both files
            List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
            List<Participant> participantParticipants = FileHandler.loadParticipantsSingleThread(participantFilePath);

            // Find highest ID and collect used IDs
            int highestId = 0;
            for (Participant p : organizerParticipants) {
                usedIds.add(p.getId());
                highestId = Math.max(highestId, extractNumericId(p.getId()));
            }
            for (Participant p : participantParticipants) {
                usedIds.add(p.getId());
                highestId = Math.max(highestId, extractNumericId(p.getId()));
            }

            // Add organizer participants first
            for (Participant p : organizerParticipants) {
                allParticipants.add(p);
                emailSet.add(p.getEmail().toLowerCase().trim());
            }

            // Add participant-added participants
            int newParticipantsAdded = 0;
            for (Participant p : participantParticipants) {
                String email = p.getEmail().toLowerCase().trim();

                if (!emailSet.contains(email)) {
                    String currentId = p.getId();
                    String newId;

                    // Check if current ID is valid and not already used
                    if (isValidParticipantId(currentId) && !usedIds.contains(currentId)) {
                        newId = currentId; // Keep original ID
                    } else {
                        newId = generateNewParticipantId(highestId);
                        highestId++;
                    }

                    Participant newParticipant = createParticipantWithNewId(p, newId);
                    allParticipants.add(newParticipant);
                    emailSet.add(email);
                    usedIds.add(newId);
                    newParticipantsAdded++;

                    System.out.println("Assigned ID " + newId + " to: " + p.getName());
                } else {
                    System.out.println("Skipping duplicate: " + p.getName() + " (" + email + ")");
                }
            }

            // Save if output path provided
            if (outputPath != null && !outputPath.isEmpty()) {
                saveMergedParticipants(allParticipants, outputPath);
                System.out.println("Merged file saved to: " + outputPath);
            }

            System.out.println("Smart merge completed! New participants: " + newParticipantsAdded);

        } catch (Exception e) {
            System.out.println("Error in smart merge: " + e.getMessage());
//            e.printStackTrace();
        }

        return allParticipants;
    }

    /**
     * Extract numeric part from ID
     */
    private static int extractNumericId(String id) {
        try {
            if (id.matches("[Pp]\\d+")) {
                return Integer.parseInt(id.substring(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }

    /**
     * Check if ID follows the expected format
     */
    private static boolean isValidParticipantId(String id) {
        return id != null && id.matches("[Pp]\\d{3}"); // P followed by 3 digits
    }

}