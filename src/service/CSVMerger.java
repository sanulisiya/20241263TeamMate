package service;

import model.Participant;
import utility.LoggerService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CSVMerger {
    private static final LoggerService logger = LoggerService.getInstance();

    // Store newly registered participants in memory
    private static final List<Participant> newParticipantsPool = new CopyOnWriteArrayList<>();

    /** Add new participant to the merge pool */
    //07.(Add new Particpnt Sequence digram)
    public static void addNewParticipant(Participant participant) {
        if (participant != null && participant.isValid()) {
            String normalizedId = normalize(participant.getId());
            boolean isDuplicate = newParticipantsPool.stream()
                    .anyMatch(p -> normalize(p.getId()).equals(normalizedId));

            if (!isDuplicate) {
                newParticipantsPool.add(participant);
                logger.info("Added new participant to merge pool - ID: " + participant.getId() + ", Email: " + participant.getEmail());
                System.out.println("Participant added to merge pool: " + participant.getId() + " - " + participant.getName());
            } else {
                logger.warn("Duplicate participant ID skipped: " + participant.getId());
                System.out.println("Participant ID already exists in merge pool. Please use a different ID.");
            }
        } else {
            logger.warn("Attempted to add invalid or null participant to merge pool");
        }
    }

    /** Get count of new participants waiting to be merged */
    public static int getNewParticipantsCount() {
        return newParticipantsPool.size();
    }

    /** Get list of new participants waiting to be merged */
    public static List<Participant> getNewParticipants() {
        return new ArrayList<>(newParticipantsPool);
    }

    /** Clear the new participants pool (after successful merge) */
    public static void clearNewParticipants() {
        int count = newParticipantsPool.size();
        newParticipantsPool.clear();
        logger.info("Cleared " + count + " participants from merge pool");
    }


    //SIMPLE MERGE: Check if new participant ID exists in organizer file
    //06.(Team formation Sequance digram)
    public static List<Participant> mergeNewParticipants(String organizerFilePath, String outputPath) {
        List<Participant> allParticipants = new ArrayList<>();

        try {
            //  Load participants from organizer file
            List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
            System.out.println(" Loaded " + organizerParticipants.size() + " participants from organizer file");


            allParticipants.addAll(organizerParticipants);

            Set<String> existingIds = new HashSet<>();
            for (Participant p : organizerParticipants) {
                if (p != null && p.getId() != null) {
                    existingIds.add(normalize(p.getId()));
                }
            }

            //  Check each new participant
            int mergedCount = 0;
            int skippedCount = 0;

            System.out.println("\n Checking " + getNewParticipantsCount() + " new participants for merge:");

            for (Participant newParticipant : newParticipantsPool) {
                if (newParticipant == null || newParticipant.getId() == null) {
                    System.out.println("SKIPPED: Invalid participant");
                    skippedCount++;
                    continue;
                }

                String newId = newParticipant.getId();
                String normalizedNewId = normalize(newId);

                System.out.println("Checking new participant ID: " + newId + " -> normalized: " + normalizedNewId);

                if (existingIds.contains(normalizedNewId)) {
                    // ID already exists - don't merge
                    System.out.println("SKIPPED: ID '" + newId + "' already exists in organizer file - " + newParticipant.getName());
                    skippedCount++;
                } else {
                    // ID doesn't exist - merge it
                    allParticipants.add(newParticipant);
                    existingIds.add(normalizedNewId);
                    mergedCount++;
                    System.out.println(" \nMERGED: " + newId + " - " + newParticipant.getName());
                }
            }

            // 5. Save the merged file to Desktop
            saveMergedParticipants(allParticipants, outputPath);

            // 6. Clear the pool if we merged any participants
            if (mergedCount > 0) {
                clearNewParticipants();
                System.out.println("Cleared " + mergedCount + " participants from merge pool");
            }

            System.out.println("\nMerge Summary:");
            System.out.println(" Merged: " + mergedCount + " participants");
            System.out.println(" Skipped: " + skippedCount + " participants (duplicate IDs)");
            System.out.println("   Total: " + allParticipants.size() + " participants in merged file");
            System.out.println("   Saved to: " + outputPath);

        } catch (Exception e) {
            System.out.println("Error during merge: " + e.getMessage());
            e.printStackTrace();
        }

        return allParticipants;
    }

    /**
     * Handle merge with options (for organizer menu)
     */
    //2.(Team formation Sequance digram)
    public static List<Participant> mergeWithOptions(String organizerFilePath, String outputPath, Scanner scanner) { //2.2.(SD-Team Formation)
        System.out.println("\n" + "=".repeat(70));
        System.out.println("New participants waiting: " + getNewParticipantsCount());

        if (getNewParticipantsCount() == 0) { //2.3.(SD-Team Formation)
            System.out.println("No new participants to merge. Using organizer file only."); //2.4.(SD-Team Formation)
            try {
                List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
                saveMergedParticipants(organizerParticipants, outputPath);
                return organizerParticipants;
            } catch (Exception e) {
                System.out.println("Error loading organizer file: " + e.getMessage());
                return new ArrayList<>();
            }
        }

        System.out.print("Do you want to merge new participants? (yes/no): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("yes") || choice.equals("y")) {
            return mergeNewParticipants(organizerFilePath, outputPath);
        } else {
            System.out.println("Skipping merge. Using organizer file only.");
            try {
                List<Participant> organizerParticipants = FileHandler.loadParticipantsSingleThread(organizerFilePath);
                saveMergedParticipants(organizerParticipants, outputPath);
                return organizerParticipants;
            } catch (Exception e) {
                System.out.println(" Error loading organizer file: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    /** Save merged participants to a new CSV file on Desktop */
    public static void saveMergedParticipants(List<Participant> participants, String outputPath) {
        try {
            // Ensure the directory exists
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileWriter writer = new FileWriter(outputFile);
            // Write CSV header
            writer.write("ID,Name,Email,PreferredGame,SkillLevel,Role,PersonalityScore,PersonalityType,TeamNumber\n");

            // Write each participant
            for (Participant p : participants) {
                String line = String.join(",",
                        p.getId(),
                        p.getName(),
                        p.getEmail(),
                        p.getPreferredGame(),
                        String.valueOf(p.getSkillLevel()),
                        p.getPreferredRole().name(),
                        String.valueOf(p.getPersonalityScore()),
                        p.getPersonalityType().name(),
                        p.getTeamNumber() != null ? p.getTeamNumber() : ""
                );
                writer.write(line + "\n");
            }
            writer.close();

            System.out.println(" Successfully saved " + participants.size() + " participants to: " + outputPath);

        } catch (IOException e) {
            System.out.println(" Error saving merged CSV: " + e.getMessage());
            throw new RuntimeException("Failed to save merged CSV file", e);
        }
    }

    // ----------------- Helpers -----------------
    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}