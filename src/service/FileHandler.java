package service;

import model.Participant;
import model.RoleType;
import model.PersonalityType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileHandler {

    // --- Threading Configuration ---
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunk size
    private static final int THREAD_POOL_SIZE = 4;

    /**
     * Runnable task that implements Callable to process a chunk of the file
     * and extract participants.
     */
    private static class ParticipantLoaderTask implements Callable<List<Participant>> {
        private final String filePath;
        private final long start;
        private final long length;
        private final List<Participant> participants = new ArrayList<>();

        public ParticipantLoaderTask(String filePath, long start, long length) {
            this.filePath = filePath;
            this.start = start;
            this.length = length;
        }

        @Override
        public List<Participant> call() throws Exception {
            try (RandomAccessFile raf = new RandomAccessFile(new File(filePath), "r")) {
                raf.seek(start);

                // If we are not at the beginning of the file, we must skip the partial line
                // found at the beginning of this chunk to ensure we start on a new record.
                if (start != 0) {
                    raf.readLine();
                } else {
                    // This is the first chunk (start == 0), so we must skip the CSV header line.
                    raf.readLine();
                }

                long currentPosition = raf.getFilePointer();
                long endLimit = start + length;

                String line;
                // Read lines until we hit the end of the assigned chunk
                while (currentPosition < endLimit && (line = raf.readLine()) != null) {
                    processLine(line);
                    currentPosition = raf.getFilePointer();
                }

            }
            return participants;
        }

        private void processLine(String line) {
            String[] data = line.split(",");
            if (data.length < 8) return;

            try {

                // Convert string to GameRole enum
                RoleType preferredRole = parseGameRole(data[5].trim());

                // Convert string to PersonalityType enum
                PersonalityType personalityType = parsePersonalityType(data[7].trim());

                if (!ParticipantValidator.validateID(data[0].trim())) {
                    System.out.println("Invalid ID: " + data[0].trim());
                    return;
                }

                if (!ParticipantValidator.validateName(data[1].trim())){
                    System.out.println("Invalid name: " + data[1].trim());
                    return;

                }
                if (!ParticipantValidator.validateEmail(data[2].trim())) {
                    System.out.println("Invalid email: " + data[2].trim());
                    return;
                }
                if (!ParticipantValidator.validateGame(data[3].trim())) {
                    System.out.println("Invalid Preffered gmae: " + data[3].trim());
                    return;
                }
                if (!ParticipantValidator.validateSkillLevel(Integer.parseInt(data[4].trim()))) {
                    System.out.println("Invalid skill level : " + data[4].trim());
                    return;
                }
                if (!ParticipantValidator.validateGame((data[5].trim()))) {
                    System.out.println("Invalid skill level : " + data[5].trim());
                    return;
                }


















































                Participant p = new Participant(
                        data[0].trim(),
                        data[1].trim(),
                        data[2].trim(),
                        data[3].trim(),
                        Integer.parseInt(data[4].trim()),
                        preferredRole,
                        Integer.parseInt(data[6].trim()),
                        personalityType
                );
                participants.add(p);
            } catch (Exception e) {
                // Log error without stopping the thread
                System.err.println(" Skipping invalid participant row in thread: " + line);
                System.err.println(" Error: " + e.getMessage());
            }
        }

        private RoleType parseGameRole(String roleString) {
            try {
                return RoleType.valueOf(roleString.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid game role: " + roleString + ", defaulting to STRATEGIST");
                return RoleType.STRATEGIST; // Default value
            }
        }

        private PersonalityType parsePersonalityType(String typeString) {
            try {
                return PersonalityType.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid personality type: " + typeString + ", defaulting to BALANCED");
                return PersonalityType.BALANCED; // Default value
            }
        }
    }

    // --- Public Multi-Threaded Methods ---

    /**
     * Load participants from original CSV using multiple threads to process file chunks.
     * @param filePath The path to the CSV file.
     * @return A list of all loaded Participant objects.
     */
    public static List<Participant> loadParticipants(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            System.err.println("File not found or is a directory: " + filePath);
            return new ArrayList<>();
        }
        long fileLength = file.length();

        List<Callable<List<Participant>>> tasks = new ArrayList<>();
        // Use an ExecutorService to manage the thread pool
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        long currentPosition = 0;
        // Split the file into chunks and create a task for each
        while (currentPosition < fileLength) {
            long length = Math.min(CHUNK_SIZE, fileLength - currentPosition);

            tasks.add(new ParticipantLoaderTask(filePath, currentPosition, length));

            currentPosition += length;
        }

        List<Participant> allParticipants = new ArrayList<>();
        try {
            // Execute all tasks and wait for them to complete
            List<Future<List<Participant>>> futures = executor.invokeAll(tasks);

            // Collect results from all threads
            for (Future<List<Participant>> future : futures) {
                // future.get() will throw an ExecutionException if the thread failed
                allParticipants.addAll(future.get());
            }

        } catch (InterruptedException e) {
            // Restore interrupt status for proper thread handling
            Thread.currentThread().interrupt();
            System.err.println("File reading interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("An error occurred during task execution: " + e.getCause().getMessage());
        } finally {
            // Always shut down the executor service
            executor.shutdown();
        }

        return allParticipants;
    }

    // --- Updated Single-Threaded Methods ---

    // Append new participant
    public static void saveParticipant(String filePath, Participant participant) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(
                    participant.getId() + "," +
                            participant.getName() + "," +
                            participant.getEmail() + "," +
                            participant.getPreferredGame() + "," +
                            participant.getSkillLevel() + "," +
                            participant.getPreferredRole().name() + "," + // Use enum name
                            participant.getPersonalityScore() + "," +
                            participant.getPersonalityType().name() + "\n" // Use enum name
            );
        } catch (IOException e) {
            System.err.println(" Error writing to file: " + e.getMessage());
        }
    }

    // Load formatted team data with team numbers
    public static List<List<Participant>> loadFormattedTeams(String filePath) {
        List<List<Participant>> teams = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            java.util.Map<Integer, List<Participant>> teamMap = new java.util.HashMap<>();

            while ((line = br.readLine()) != null) {
                if (isHeader) { // skip header if exists
                    isHeader = false;
                    if (line.toLowerCase().contains("team")) continue;
                }

                String[] data = line.split(",");
                if (data.length < 9) continue; // skip invalid rows

                int teamNumber;
                try {
                    teamNumber = Integer.parseInt(data[0].trim());
                } catch (NumberFormatException e) {
                    continue; // skip bad rows
                }

                try {
                    // Convert string to GameRole enum
                    RoleType preferredRole = parseGameRole(data[6].trim());

                    // Convert string to PersonalityType enum
                    PersonalityType personalityType = parsePersonalityType(data[8].trim());


                    Participant p = new Participant(
                            data[1].trim(),
                            data[2].trim(),
                            data[3].trim(),
                            data[4].trim(),
                            Integer.parseInt(data[5].trim()),
                            preferredRole,  // Now using GameRole enum
                            Integer.parseInt(data[7].trim()),
                            personalityType  // Now using PersonalityType enum
                    );

                    // Set team number using the setter method
                    p.setTeamNumber(String.valueOf(teamNumber));

                    teamMap.computeIfAbsent(teamNumber, k -> new ArrayList<>()).add(p);

                } catch (Exception e) {
                    System.err.println(" Skipping invalid team row: " + line);
                    System.err.println(" Error: " + e.getMessage());
                }
            }

            teams.addAll(teamMap.values());

        } catch (IOException e) {
            System.err.println("Error reading formatted team file: " + e.getMessage());
        }

        return teams;
    }

    // Helper methods for parsing enums from strings
    private static RoleType parseGameRole(String roleString) {
        try {
            return RoleType.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid game role: " + roleString + ", defaulting to STRATEGIST");
            return RoleType.STRATEGIST; // Default value
        }
    }

    private static PersonalityType parsePersonalityType(String typeString) {
        try {
            return PersonalityType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid personality type: " + typeString + ", defaulting to BALANCED");
            return PersonalityType.BALANCED; // Default value
        }
    }
}