// File: service/CSVMerger.java
package service;

import model.Participant;
import java.io.*;
import java.util.*;

public class CSVMerger {

    /**
     * Merges two CSV files, removing duplicates based on email address
     * @param filePath1 First CSV file path (participant-added)
     * @param filePath2 Second CSV file path (organizer-uploaded)  
     * @param outputPath Output merged CSV file path
     * @return List of all unique participants from both files
     */
    public static List<Participant> mergeCSVFiles(String filePath1, String filePath2, String outputPath) {
        List<Participant> allParticipants = new ArrayList<>();
        Set<String> emailSet = new HashSet<>(); // To track unique emails

        try {
            // Load participants from first file
            List<Participant> participants1 = FileHandler.loadParticipantsSingleThread(filePath1);
            System.out.println("Loaded " + participants1.size() + " participants from: " + filePath1);

            // Load participants from second file
            List<Participant> participants2 = FileHandler.loadParticipantsSingleThread(filePath2);
            System.out.println("Loaded " + participants2.size() + " participants from: " + filePath2);

            // Merge participants, removing duplicates by email
            mergeParticipants(allParticipants, emailSet, participants1);
            mergeParticipants(allParticipants, emailSet, participants2);

            // Save merged list to output file
            saveMergedParticipants(allParticipants, outputPath);

            System.out.println("Successfully merged " + allParticipants.size() + " unique participants");
            System.out.println("Merged file saved to: " + outputPath);

        } catch (Exception e) {
            System.out.println("Error merging CSV files: " + e.getMessage());
            e.printStackTrace();
        }

        return allParticipants;
    }

    /**
     * Merge participants into the main list, checking for duplicates by email
     */
    private static void mergeParticipants(List<Participant> allParticipants,
                                          Set<String> emailSet,
                                          List<Participant> newParticipants) {
        for (Participant p : newParticipants) {
            String email = p.getEmail().toLowerCase().trim();

            if (!emailSet.contains(email)) {
                // New unique participant - add to list
                allParticipants.add(p);
                emailSet.add(email);
            } else {
                // Duplicate email found - you can choose to handle this differently
                // For now, we just skip duplicates
                System.out.println("Skipping duplicate participant with email: " + email);
            }
        }
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

        } catch (IOException e) {
            System.out.println("Error saving merged CSV: " + e.getMessage());
            throw new RuntimeException("Failed to save merged CSV file", e);
        }
    }

    /**
     * Quick merge without saving to file - just returns merged list
     */
    public static List<Participant> quickMerge(String filePath1, String filePath2) {
        List<Participant> allParticipants = new ArrayList<>();
        Set<String> emailSet = new HashSet<>();

        try {
            List<Participant> participants1 = FileHandler.loadParticipantsSingleThread(filePath1);
            List<Participant> participants2 = FileHandler.loadParticipantsSingleThread(filePath2);

            mergeParticipants(allParticipants, emailSet, participants1);
            mergeParticipants(allParticipants, emailSet, participants2);

        } catch (Exception e) {
            System.out.println("Error in quick merge: " + e.getMessage());
        }

        return allParticipants;
    }
}